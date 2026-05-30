package com.example.authservice.mapper;

import com.example.authservice.dto.AuthUserResponse;
import com.example.authservice.entity.AuthUserEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthMapper {

    public AuthUserResponse toUserResponse(AuthUserEntity entity) {
        List<String> roles = entity.getUserRoles() == null ? List.of() :
                entity.getUserRoles().stream()
                        .map(ur -> ur.getRole().getName())
                        .toList();
        return new AuthUserResponse(
                entity.getId(),
                entity.getFullName(),
                entity.getPhoneNumber(),
                entity.getEmail(),  // optional
                entity.getStatus(),
                entity.getCreatedAt(),
                roles
        );
    }
}
