package com.example.authservice.client;

import com.example.authservice.config.UserServiceProperties;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class UserServiceClient {
    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);
    private static final String USERS_PATH = "/api/v1/users";

    private final RestTemplate restTemplate;
    private final URI usersUri;

    public UserServiceClient(RestTemplateBuilder restTemplateBuilder, UserServiceProperties properties) {
        this.restTemplate = restTemplateBuilder.build();
        this.usersUri = URI.create(properties.baseUrl()).resolve(USERS_PATH);
    }

    /**
     * Best-effort profile creation.
     *
     * In this repo the mobile app should always call the gateway, and the gateway provisions the user profile
     * using the internal endpoint. This client exists only as a fallback and must not break registration
     * if user-service is down or protected.
     */
    public void createUser(String fullName, String phoneNumber, String email) {
        // If email is blank, generate placeholder from phone
        if (email == null || email.isBlank()) {
            email = phoneNumber + "@phone.invalid";
        }
        UserCreateUserRequest request = new UserCreateUserRequest(fullName, email, phoneNumber);
        try {
            restTemplate.postForLocation(usersUri, request);
        } catch (RestClientException ex) {
            log.warn("Unable to create user profile after registration (best-effort).", ex);
        }
    }

    private record UserCreateUserRequest(String fullName, String email, String phoneNumber) {
    }
}
