package com.example.authservice.repository;

import com.example.authservice.entity.AuthUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUserEntity, Long> {

    Optional<AuthUserEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<AuthUserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM AuthUserEntity u " +
           "LEFT JOIN FETCH u.userRoles ur " +
           "LEFT JOIN FETCH ur.role " +
           "WHERE u.id = :userId")
    Optional<AuthUserEntity> findByIdWithRoles(Long userId);
}
