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
import com.example.authservice.exception.AuthenticationFailedException;
import com.example.authservice.exception.ResourceConflictException;
import com.example.authservice.exception.TokenValidationException;
import com.example.authservice.mapper.AuthMapper;
import com.example.authservice.repository.AuthSessionRepository;
import com.example.authservice.repository.AuthUserRepository;
import com.example.authservice.client.UserServiceClient;
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
                userServiceClient
        );
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(authUserRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(new AuthUserEntity()));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Test User", "test@example.com", "Password123")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("A user with this email already exists.");
    }

    @Test
    void loginRejectsInvalidPassword() {
        AuthUserEntity user = new AuthUserEntity();
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("encoded");
        user.setStatus("ACTIVE");
        when(authUserRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password.");
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
        when(authUserRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");
        when(authUserRepository.save(any(AuthUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenService.generateOtpCode()).thenReturn("123456");
        when(tokenService.hashOtpCode("123456")).thenReturn("otp-hash");

        var response = authService.register(new RegisterRequest("Test User", "test@example.com", "Password123"));

        assertThat(response.verificationRequired()).isTrue();
        assertThat(response.otpCode()).isEqualTo("123456");
        assertThat(response.user().email()).isEqualTo("test@example.com");
        assertThat(response.user().status()).isEqualTo("PENDING_VERIFICATION");
    }
}
