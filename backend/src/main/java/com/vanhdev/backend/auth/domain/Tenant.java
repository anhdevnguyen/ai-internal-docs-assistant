package com.vanhdev.backend.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    // URL-safe identifier used in subdomain routing and API scoping
    @Column(unique = true, nullable = false)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Tenant() {}

    public Tenant(String name, String slug) {
        this.name      = name;
        this.slug      = slug;
        this.createdAt = Instant.now();
    }
}