package com.example.authservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auth_users")
public class AuthUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "email", unique = true, length = 160)
    private String email;  // Optional now

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "otp_code_hash", length = 64)
    private String otpCodeHash;

    @Column(name = "otp_expires_at")
    private OffsetDateTime otpExpiresAt;

    @Column(name = "otp_sent_at")
    private OffsetDateTime otpSentAt;

    @Column(name = "otp_attempts", nullable = false)
    private Integer otpAttempts = 0;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRoleEntity> userRoles = new ArrayList<>();

    @Transient
    private List<String> roles = new ArrayList<>();

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public OffsetDateTime getLockedUntil() { return lockedUntil; }
    public String getOtpCodeHash() { return otpCodeHash; }
    public OffsetDateTime getOtpExpiresAt() { return otpExpiresAt; }
    public OffsetDateTime getOtpSentAt() { return otpSentAt; }
    public Integer getOtpAttempts() { return otpAttempts; }
    public List<UserRoleEntity> getUserRoles() { return userRoles; }
    public List<String> getRoles() { return roles; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public void setLockedUntil(OffsetDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public void setOtpCodeHash(String otpCodeHash) { this.otpCodeHash = otpCodeHash; }
    public void setOtpExpiresAt(OffsetDateTime otpExpiresAt) { this.otpExpiresAt = otpExpiresAt; }
    public void setOtpSentAt(OffsetDateTime otpSentAt) { this.otpSentAt = otpSentAt; }
    public void setOtpAttempts(Integer otpAttempts) { this.otpAttempts = otpAttempts; }
    public void setUserRoles(List<UserRoleEntity> userRoles) { this.userRoles = userRoles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    // Convenience methods
    public void addRole(RoleEntity role) {
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUser(this);
        userRole.setRole(role);
        userRole.setId(new UserRoleId(this.id, role.getId()));
        this.userRoles.add(userRole);
    }

    public void clearRoles() {
        this.userRoles.clear();
    }
}
