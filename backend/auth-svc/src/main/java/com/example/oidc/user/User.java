package com.example.oidc.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "internal_users", uniqueConstraints =
        @UniqueConstraint(name = "uk_internal_users_identity", columnNames = {"issuer", "subject"}))
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, length = 512, updatable = false)
    private String issuer;

    @Column(nullable = false, length = 255, updatable = false)
    private String subject;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 320)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    User(String issuer, String subject, Instant now) {
        this.id = UUID.randomUUID();
        this.issuer = issuer;
        this.subject = subject;
        this.createdAt = now;
    }

    void updateProfile(String name, String email, Instant now) {
        this.name = name;
        this.email = email;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
