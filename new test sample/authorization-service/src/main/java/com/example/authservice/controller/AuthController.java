package com.example.authservice.controller;

import com.example.authservice.dto.AuthTokensResponse;
import com.example.authservice.dto.HealthResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LogoutRequest;
import com.example.authservice.dto.RefreshTokenRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.dto.RegisterResponse;
import com.example.authservice.dto.VerifyOtpRequest;
import com.example.authservice.service.AuthService;
import com.example.authservice.dto.AuthUserResponse;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(
                "AUTH_SERVICE_READY",
                "Authorization service is available.",
                OffsetDateTime.now()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthTokensResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<AuthTokensResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> logoutAll(@RequestHeader("Authorization") String authorizationHeader) {
        authService.logoutAll(authorizationHeader);
        return ResponseEntity.noContent().build();
    }

     @GetMapping("/me")
     @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
     public ResponseEntity<AuthUserResponse> getMe(@RequestHeader("Authorization") String authorizationHeader) {
         return ResponseEntity.ok(authService.getMe(authorizationHeader));
     }
 }
