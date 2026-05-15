package com.vanhdev.backend.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_users_tenant_email",
                columnNames = {"tenant_id", "email"}
        )
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Default is set in the constructor, not at field declaration,
    // so the IDE does not warn about the field-level default being redundant.
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {}

    public User(UUID tenantId, String email, String passwordHash, Role role) {
        this.tenantId     = tenantId;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.active       = true;
        this.createdAt    = Instant.now();
        this.updatedAt    = this.createdAt;
    }

    // --- Domain behaviour ---

    public void deactivate() {
        this.active    = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active    = true;
        this.updatedAt = Instant.now();
    }

    public void promoteToAdmin() {
        this.role      = Role.ADMIN;
        this.updatedAt = Instant.now();
    }
}