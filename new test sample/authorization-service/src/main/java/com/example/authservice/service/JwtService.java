package com.example.authservice.service;

import com.example.authservice.config.JwtProperties;
import com.example.authservice.dto.AuthUserResponse;
import com.example.authservice.entity.AuthUserEntity;
import com.example.authservice.entity.UserRoleEntity;
import com.example.authservice.exception.TokenValidationException;
import com.example.authservice.mapper.AuthMapper;
import com.example.authservice.repository.UserRoleRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AuthMapper authMapper;
    private final UserRoleRepository userRoleRepository;

    public JwtService(JwtProperties jwtProperties, JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, AuthMapper authMapper, UserRoleRepository userRoleRepository) {
        this.jwtProperties = jwtProperties;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.authMapper = authMapper;
        this.userRoleRepository = userRoleRepository;
    }

    public AccessTokenIssue issueAccessToken(AuthUserEntity user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofMinutes(jwtProperties.getAccessTtlMinutes()));

        // Load user roles
        List<String> roleNames = userRoleRepository.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toList());
        user.setRoles(roleNames);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .claim("phone", user.getPhoneNumber())
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
                .claim("roles", roleNames)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessTokenIssue(tokenValue, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC), authMapper.toUserResponse(user));
    }

    public DecodedAccessToken decodeBearer(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new TokenValidationException("Authorization header is required.");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new TokenValidationException("Authorization header must be Bearer token.");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        try {
            Jwt jwt = jwtDecoder.decode(token);
            long userId = Long.parseLong(jwt.getSubject());
            return new DecodedAccessToken(userId, jwt);
        } catch (Exception exception) {
            throw new TokenValidationException("Access token is invalid.");
        }
    }

    public record AccessTokenIssue(String token, OffsetDateTime expiresAt, AuthUserResponse user) {}

    public record DecodedAccessToken(long userId, Jwt jwt) {}
}
