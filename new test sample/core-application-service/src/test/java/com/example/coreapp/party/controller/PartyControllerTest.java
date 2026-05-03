package com.example.coreapp.party.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.coreapp.common.api.GlobalExceptionHandler;
import com.example.coreapp.party.service.PartyService;
import com.example.coreapp.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@Import({PartyControllerTest.JwtTestConfig.class, SecurityConfig.class, GlobalExceptionHandler.class})
@WebMvcTest(controllers = PartyController.class)
class PartyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartyService partyService;

    @TestConfiguration
    static class JwtTestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "1")
                    .build();
        }
    }

    @Test
    void list_requires_auth() throws Exception {
        mockMvc.perform(get("/api/v1/parties"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_requires_auth() throws Exception {
        mockMvc.perform(post("/api/v1/parties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"type\":\"CUSTOMER\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_validation_fails_on_missing_fields() throws Exception {
        mockMvc.perform(post("/api/v1/parties")
                        .with(jwt().jwt(j -> j.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
