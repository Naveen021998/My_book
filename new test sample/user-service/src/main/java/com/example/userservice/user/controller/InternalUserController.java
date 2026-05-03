package com.example.userservice.user.controller;

import com.example.userservice.user.dto.ProvisionUserRequest;
import com.example.userservice.user.dto.UserResponse;
import com.example.userservice.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal-only endpoints.
 *
 * Protected by {@code X-Internal-Token} (see security filter).
 */
@RestController
@RequestMapping("/api/v1/users/internal")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/provision")
    public ResponseEntity<UserResponse> provision(@Valid @RequestBody ProvisionUserRequest request) {
        // Idempotent: if profile exists, returns existing.
        return ResponseEntity.ok(userService.provisionUser(request));
    }
}

