package com.example.authservice.service;

import com.example.authservice.client.UserServiceClient;
import com.example.authservice.dto.AuthTokensResponse;
import com.example.authservice.dto.AuthUserResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LogoutRequest;
import com.example.authservice.dto.RefreshTokenRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.dto.RegisterResponse;
import com.example.authservice.dto.VerifyOtpRequest;
import com.example.authservice.entity.AuthSessionEntity;
import com.example.authservice.entity.AuthUserEntity;
import com.example.authservice.entity.RoleEntity;
import com.example.authservice.entity.UserRoleEntity;
import com.example.authservice.entity.UserRoleId;
import com.example.authservice.exception.AuthenticationFailedException;
import com.example.authservice.exception.ResourceConflictException;
import com.example.authservice.exception.TokenValidationException;
import com.example.authservice.mapper.AuthMapper;
import com.example.authservice.repository.AuthSessionRepository;
import com.example.authservice.repository.AuthUserRepository;
import com.example.authservice.repository.RoleRepository;
import com.example.authservice.repository.UserRoleRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);

    private final AuthUserRepository authUserRepository;
    private final AuthSessionRepository authSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final UserServiceClient userServiceClient;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public AuthService(
            AuthUserRepository authUserRepository,
            AuthSessionRepository authSessionRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            JwtService jwtService,
            AuthMapper authMapper,
            UserServiceClient userServiceClient,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.authUserRepository = authUserRepository;
        this.authSessionRepository = authSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtService = jwtService;
        this.authMapper = authMapper;
        this.userServiceClient = userServiceClient;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        try {
            String phoneNumber = normalizePhone(request.phoneNumber());

            // Check if any user with this phone exists
            boolean phoneExists = authUserRepository.existsByPhoneNumber(phoneNumber);
            if (phoneExists) {
                throw new ResourceConflictException("A user with this phone number already exists.");
            }

            AuthUserEntity user = new AuthUserEntity();
            user.setPhoneNumber(phoneNumber);
            user.setFullName(request.fullName().trim());
            user.setEmail(null);  // Email optional
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setStatus("PENDING_VERIFICATION");
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);

            String otpCode = tokenService.generateOtpCode();
            user.setOtpCodeHash(tokenService.hashOtpCode(otpCode));
            user.setOtpExpiresAt(OffsetDateTime.now().plus(OTP_TTL));
            user.setOtpSentAt(OffsetDateTime.now());
            user.setOtpAttempts(0);

            AuthUserEntity savedUser = authUserRepository.save(user);
            if (savedUser == null) {
                throw new IllegalStateException("Failed to save user");
            }

            // Assign default USER role
            RoleEntity userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalStateException("Default USER role not found in database"));
            UserRoleEntity userRoleEntity = new UserRoleEntity();
            userRoleEntity.setUser(savedUser);
            userRoleEntity.setRole(userRole);
            userRoleEntity.setId(new UserRoleId(savedUser.getId(), userRole.getId()));
            userRoleRepository.save(userRoleEntity);

            savedUser.setRoles(List.of("USER"));

            // Create user profile in user-service (best-effort)
            try {
                userServiceClient.createUser(savedUser.getFullName(), savedUser.getPhoneNumber(), savedUser.getEmail());
            } catch (Exception e) {
                // Log but don't fail registration
            }

            return new RegisterResponse(
                    true,
                    otpCode,
                    savedUser.getOtpExpiresAt(),
                    "Verification code generated. Confirm the OTP to continue.",
                    authMapper.toUserResponse(savedUser)
            );
        } catch (DataIntegrityViolationException ex) {
            // Race condition: phone uniqueness constraint violation
            throw new ResourceConflictException("This phone number is already registered.");
        }
    }

    @Transactional
    public AuthTokensResponse verifyOtp(VerifyOtpRequest request) {
        String phoneNumber = normalizePhone(request.phoneNumber());
        AuthUserEntity user = authUserRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid phone number or verification code."));

        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AuthenticationFailedException("Account is already verified. Please login.");
        }

        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AuthenticationFailedException("Verification code has expired. Please register again.");
        }

        String expectedHash = user.getOtpCodeHash();
        if (expectedHash == null || !expectedHash.equals(tokenService.hashOtpCode(request.otpCode()))) {
            int attempts = user.getOtpAttempts() == null ? 0 : user.getOtpAttempts();
            user.setOtpAttempts(attempts + 1);
            throw new AuthenticationFailedException("Invalid verification code.");
        }

        user.setStatus("ACTIVE");
        user.setOtpCodeHash(null);
        user.setOtpExpiresAt(null);
        user.setOtpSentAt(null);
        user.setOtpAttempts(0);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        AuthUserEntity savedUser = authUserRepository.save(user);
        try {
            userServiceClient.createUser(savedUser.getFullName(), savedUser.getPhoneNumber(), savedUser.getEmail());
        } catch (Exception e) {
            // best-effort, ignore
        }
        return createSessionResponse(savedUser);
    }

    @Transactional
    public AuthTokensResponse login(LoginRequest request) {
        String phoneNumber = normalizePhone(request.phoneNumber());
        AuthUserEntity user = authUserRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid phone number or password."));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new AuthenticationFailedException("Account is temporarily locked. Try again later.");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AuthenticationFailedException("Please verify your account first.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
            attempts++;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setLockedUntil(OffsetDateTime.now().plus(Duration.ofMinutes(15)));
                user.setFailedLoginAttempts(0);
            }
            throw new AuthenticationFailedException("Invalid phone number or password.");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        return createSessionResponse(user);
    }

    @Transactional
    public AuthTokensResponse refresh(RefreshTokenRequest request) {
        String refreshTokenHash = tokenService.hashToken(request.refreshToken());
        AuthSessionEntity session = authSessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new TokenValidationException("Refresh token is invalid."));

        validateRefreshSession(session);

        session.setRevokedAt(OffsetDateTime.now());
        session.setLastUsedAt(OffsetDateTime.now());

        return createSessionResponse(session.getUser());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String refreshTokenHash = tokenService.hashToken(request.refreshToken());
        AuthSessionEntity session = authSessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new TokenValidationException("Refresh token is invalid."));

        validateRefreshSession(session);
        session.setRevokedAt(OffsetDateTime.now());
        session.setLastUsedAt(OffsetDateTime.now());
    }

    @Transactional
    public void logoutAll(String authorizationHeader) {
        long userId = jwtService.decodeBearer(authorizationHeader).userId();
        AuthUserEntity user = authUserRepository.findById(userId)
                .orElseThrow(() -> new TokenValidationException("User not found for token."));
        authSessionRepository.revokeAllActiveForUser(user, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getMe(String authorizationHeader) {
        long userId = jwtService.decodeBearer(authorizationHeader).userId();
        AuthUserEntity user = authUserRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new TokenValidationException("User not found for token."));
        return authMapper.toUserResponse(user);
    }

    private void validateRefreshSession(AuthSessionEntity session) {
        if (session.getRevokedAt() != null) {
            authSessionRepository.revokeAllActiveForUser(session.getUser(), OffsetDateTime.now());
            throw new TokenValidationException("Refresh token has already been revoked.");
        }
        if (session.getRefreshTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new TokenValidationException("Refresh token has expired.");
        }
    }

    private AuthTokensResponse createSessionResponse(AuthUserEntity user) {
        String refreshToken = tokenService.generateToken();
        JwtService.AccessTokenIssue accessTokenIssue = jwtService.issueAccessToken(user);

        AuthSessionEntity session = new AuthSessionEntity();
        session.setUser(user);
        session.setAccessTokenHash(tokenService.hashToken(accessTokenIssue.token()));
        session.setRefreshTokenHash(tokenService.hashToken(refreshToken));
        session.setAccessTokenExpiresAt(accessTokenIssue.expiresAt());
        session.setRefreshTokenExpiresAt(tokenService.refreshExpiry());
        session.setRevokedAt(null);
        session.setLastUsedAt(OffsetDateTime.now());

        AuthSessionEntity savedSession = authSessionRepository.save(session);
        return new AuthTokensResponse(
                accessTokenIssue.token(),
                savedSession.getAccessTokenExpiresAt(),
                refreshToken,
                savedSession.getRefreshTokenExpiresAt(),
                "Bearer",
                accessTokenIssue.user()
        );
    }

    private String normalizePhone(String phone) {
        String cleaned = phone.trim();

        // Auto-prepend +91 for 10-digit local Indian numbers (starting with 6-9)
        if (!cleaned.startsWith("+")) {
            String digitsOnly = cleaned.replaceAll("\\D", "");
            if (digitsOnly.length() == 10 && digitsOnly.matches("^[6-9]\\d{9}$")) {
                cleaned = "+91" + digitsOnly;
            } else {
                cleaned = "+" + digitsOnly;
            }
        }

        // Remove spaces/dashes after +
        cleaned = cleaned.replaceAll("[\\s-]", "");

        return cleaned;
    }
}
