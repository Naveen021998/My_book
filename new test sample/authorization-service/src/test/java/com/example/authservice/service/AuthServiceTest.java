package com.example.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.authservice.dto.AuthTokensResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RefreshTokenRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.dto.AuthUserResponse;
import com.example.authservice.entity.AuthSessionEntity;
import com.example.authservice.entity.AuthUserEntity;
import com.example.authservice.entity.RoleEntity;
import com.example.authservice.entity.UserRoleEntity;
import com.example.authservice.exception.AuthenticationFailedException;
import com.example.authservice.exception.ResourceConflictException;
import com.example.authservice.exception.TokenValidationException;
import com.example.authservice.mapper.AuthMapper;
import com.example.authservice.repository.AuthSessionRepository;
import com.example.authservice.repository.AuthUserRepository;
import com.example.authservice.client.UserServiceClient;
import com.example.authservice.repository.RoleRepository;
import com.example.authservice.repository.UserRoleRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authUserRepository,
                authSessionRepository,
                passwordEncoder,
                tokenService,
                jwtService,
                new AuthMapper(),
                userServiceClient,
                roleRepository,
                userRoleRepository
        );
    }

    @Test
    void registerRejectsDuplicatePhone() {
        when(authUserRepository.existsByPhoneNumber("+919876543210")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Test User", "+919876543210", "Password123")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("A user with this phone number already exists.");
    }

    @Test
    void loginRejectsInvalidPassword() {
        AuthUserEntity user = new AuthUserEntity();
        user.setFullName("Test User");
        user.setPhoneNumber("+919876543210");
        user.setPasswordHash("encoded");
        user.setStatus("ACTIVE");
        when(authUserRepository.findByPhoneNumber("+919876543210")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("+919876543210", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid phone number or password.");
    }

    @Test
    void refreshRejectsExpiredToken() {
        AuthSessionEntity session = new AuthSessionEntity();
        session.setRefreshTokenHash("hashed");
        session.setRefreshTokenExpiresAt(OffsetDateTime.now().minusMinutes(1));
        when(tokenService.hashToken("refresh")).thenReturn("hashed");
        when(authSessionRepository.findByRefreshTokenHash("hashed")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("refresh")))
                .isInstanceOf(TokenValidationException.class)
                .hasMessage("Refresh token has expired.");
    }

    @Test
    void registerCreatesOtp() {
        when(authUserRepository.existsByPhoneNumber("+919876543210")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");
        when(authUserRepository.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenService.generateOtpCode()).thenReturn("123456");
        when(tokenService.hashOtpCode("123456")).thenReturn("otp-hash");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new RoleEntity()));

        var response = authService.register(new RegisterRequest("Test User", "+919876543210", "Password123"));

        assertThat(response.verificationRequired()).isTrue();
        assertThat(response.otpCode()).isEqualTo("123456");
        assertThat(response.user().phoneNumber()).isEqualTo("+919876543210");
        assertThat(response.user().status()).isEqualTo("PENDING_VERIFICATION");
    }
}
