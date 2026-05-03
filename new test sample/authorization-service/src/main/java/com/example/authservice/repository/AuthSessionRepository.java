package com.example.authservice.repository;

import com.example.authservice.entity.AuthSessionEntity;
import com.example.authservice.entity.AuthUserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, Long> {

    Optional<AuthSessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    List<AuthSessionEntity> findAllByUserAndRevokedAtIsNull(AuthUserEntity user);

    @Modifying
    @Query("""
            update AuthSessionEntity s
               set s.revokedAt = :now,
                   s.lastUsedAt = :now
             where s.user = :user
               and s.revokedAt is null
            """)
    int revokeAllActiveForUser(@Param("user") AuthUserEntity user, @Param("now") OffsetDateTime now);
}
