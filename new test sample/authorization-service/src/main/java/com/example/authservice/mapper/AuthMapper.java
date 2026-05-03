package com.example.authservice.mapper;

import com.example.authservice.dto.AuthUserResponse;
import com.example.authservice.entity.AuthUserEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public AuthUserResponse toUserResponse(AuthUserEntity entity) {
        return new AuthUserResponse(
                entity.getId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
