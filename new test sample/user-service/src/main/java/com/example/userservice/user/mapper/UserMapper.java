package com.example.userservice.user.mapper;

import com.example.userservice.user.dto.CreateUserRequest;
import com.example.userservice.user.dto.UserResponse;
import com.example.userservice.user.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(CreateUserRequest request) {
        UserEntity entity = new UserEntity();
        entity.setFullName(request.fullName().trim());
        entity.setEmail(request.email().trim().toLowerCase());
        entity.setPhoneNumber(request.phoneNumber() == null ? null : request.phoneNumber().trim());
        entity.setStatus("ACTIVE");
        return entity;
    }

    public UserResponse toResponse(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
