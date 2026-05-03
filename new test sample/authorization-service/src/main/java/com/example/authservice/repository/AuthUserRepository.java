package com.example.authservice.repository;

import com.example.authservice.entity.AuthUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserRepository extends JpaRepository<AuthUserEntity, Long> {

    Optional<AuthUserEntity> findByEmailIgnoreCase(String email);
}
