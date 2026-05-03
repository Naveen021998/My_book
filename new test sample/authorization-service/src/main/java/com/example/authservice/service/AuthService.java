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
import com.example.authservice.exception.AuthenticationFailedException;
import com.example.authservice.exception.ResourceConflictException;
import com.example.authservice.exception.TokenValidationException;
import com.example.authservice.mapper.AuthMapper;
import com.example.authservice.repository.AuthSessionRepository;
import com.example.authservice.repository.AuthUserRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
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

    public AuthService(
            AuthUserRepository authUserRepository,
            AuthSessionRepository authSessionRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            JwtService jwtService,
            AuthMapper authMapper,
            UserServiceClient userServiceClient
    ) {
        this.authUserRepository = authUserRepository;
        this.authSessionRepository = authSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtService = jwtService;
        this.authMapper = authMapper;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        AuthUserEntity user = authUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(AuthUserEntity::new);

        if (user.getId() != null && "ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ResourceConflictException("A user with this email already exists.");
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
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
        return new RegisterResponse(
                true,
                otpCode,
                savedUser.getOtpExpiresAt(),
                "Verification code generated. Confirm the OTP to continue.",
                authMapper.toUserResponse(savedUser)
        );
    }

    @Transactional
    public AuthTokensResponse verifyOtp(VerifyOtpRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        AuthUserEntity user = authUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or verification code."));

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
        userServiceClient.createUser(savedUser.getFullName(), savedUser.getEmail());
        return createSessionResponse(savedUser);
    }

    @Transactional
    public AuthTokensResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        AuthUserEntity user = authUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password."));

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
            throw new AuthenticationFailedException("Invalid email or password.");
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
        AuthUserEntity user = authUserRepository.findById(userId)
                .orElseThrow(() -> new TokenValidationException("User not found for token."));
        return authMapper.toUserResponse(user);
    }

    private void validateRefreshSession(AuthSessionEntity session) {
        if (session.getRevokedAt() != null) {
            // Reuse detection: if a revoked refresh token is presented again, treat it as suspicious and revoke
            // all active sessions for the user (defense in depth).
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
}
