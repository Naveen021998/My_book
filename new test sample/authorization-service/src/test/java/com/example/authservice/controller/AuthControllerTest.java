package com.example.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.authservice.dto.AuthTokensResponse;
import com.example.authservice.dto.AuthUserResponse;
import com.example.authservice.service.AuthService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void healthReturnsReadyStatus() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_SERVICE_READY"));
    }

    @Test
    void registerRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "fullName": "",
                                  "email": "bad-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void loginReturnsTokens() throws Exception {
        when(authService.login(any())).thenReturn(new AuthTokensResponse(
                "access",
                OffsetDateTime.now().plusMinutes(15),
                "refresh",
                OffsetDateTime.now().plusDays(7),
                "Bearer",
                new AuthUserResponse(1L, "Test User", "test@example.com", "ACTIVE", OffsetDateTime.now())
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "test@example.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void meReturnsUser() throws Exception {
        when(authService.getMe(any())).thenReturn(new AuthUserResponse(1L, "Test User", "test@example.com", "ACTIVE", OffsetDateTime.now()));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
