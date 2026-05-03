package com.example.userservice.user.service;

import com.example.userservice.common.exception.ResourceConflictException;
import com.example.userservice.common.exception.ResourceNotFoundException;
import com.example.userservice.common.web.PagedResponse;
import com.example.userservice.user.dto.CreateUserRequest;
import com.example.userservice.user.dto.ProvisionUserRequest;
import com.example.userservice.user.dto.UpdateMeRequest;
import com.example.userservice.user.dto.UserResponse;
import com.example.userservice.user.entity.UserEntity;
import com.example.userservice.user.mapper.UserMapper;
import com.example.userservice.user.repository.UserRepository;
import com.example.userservice.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUser currentUser;

    public UserService(UserRepository userRepository, UserMapper userMapper, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.currentUser = currentUser;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        long authUserId = currentUser.userId();
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedPhone = request.phoneNumber().trim();

        if (userRepository.findByAuthUserId(authUserId).isPresent()) {
            throw new ResourceConflictException("Profile already exists for the current user.");
        }

        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ResourceConflictException("A user with this email already exists.");
        }

        if (userRepository.findByPhoneNumber(normalizedPhone).isPresent()) {
            throw new ResourceConflictException("A user with this phone number already exists.");
        }

        UserEntity user = userMapper.toEntity(request);
        user.setAuthUserId(authUserId);
        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Internal provisioning used during auth register to ensure /users/me works immediately.
     *
     * Idempotent behavior:
     * - If a profile already exists for authUserId: return it.
     * - Else if a profile exists for the email: link it to authUserId if safe.
     * - Else create a minimal profile (phone can be added later).
     */
    @Transactional
    public UserResponse provisionUser(ProvisionUserRequest request) {
        Long authUserId = request.authUserId();
        String normalizedEmail = request.email().trim().toLowerCase();

        UserEntity byAuth = userRepository.findByAuthUserId(authUserId).orElse(null);
        if (byAuth != null) {
            // Keep data reasonably in sync.
            byAuth.setFullName(request.fullName().trim());
            byAuth.setEmail(normalizedEmail);
            return userMapper.toResponse(byAuth);
        }

        UserEntity byEmail = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (byEmail != null) {
            if (byEmail.getAuthUserId() != null && !byEmail.getAuthUserId().equals(authUserId)) {
                throw new ResourceConflictException("Email is already linked to another auth account.");
            }
            byEmail.setAuthUserId(authUserId);
            byEmail.setFullName(request.fullName().trim());
            byEmail.setEmail(normalizedEmail);
            return userMapper.toResponse(byEmail);
        }

        UserEntity entity = new UserEntity();
        entity.setAuthUserId(authUserId);
        entity.setFullName(request.fullName().trim());
        entity.setEmail(normalizedEmail);
        entity.setPhoneNumber(null);
        entity.setStatus("ACTIVE");

        return userMapper.toResponse(userRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserResponse> result = userRepository.findAll(pageable)
                .map(userMapper::toResponse);

        return PagedResponse.from(result);
    }

    @Transactional(readOnly = true)
    public UserResponse getMe() {
        long authUserId = currentUser.userId();
        UserEntity entity = userRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found."));
        return userMapper.toResponse(entity);
    }

    @Transactional
    public UserResponse updateMe(UpdateMeRequest request) {
        long authUserId = currentUser.userId();
        UserEntity entity = userRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found."));

        if (request.fullName() != null) {
            entity.setFullName(request.fullName().trim());
        }
        if (request.phoneNumber() != null) {
            String normalizedPhone = request.phoneNumber().trim();
            if (!normalizedPhone.equals(entity.getPhoneNumber())
                    && userRepository.findByPhoneNumber(normalizedPhone).isPresent()) {
                throw new ResourceConflictException("A user with this phone number already exists.");
            }
            entity.setPhoneNumber(normalizedPhone);
        }

        return userMapper.toResponse(entity);
    }
}
