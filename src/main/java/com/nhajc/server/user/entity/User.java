package com.nhajc.server.user.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String username;

    @Column(nullable = false, columnDefinition = "text")
    private String email;

    @Column(nullable = false, columnDefinition = "text")
    private String password;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(columnDefinition = "bytea")
    private byte[] avatar;

    @Column(name = "avatar_content_type", length = 10)
    private String avatarContentType;

    @Column(name = "avatar_object_key", columnDefinition = "text")
    private String avatarObjectKey;

    protected User() {
    }

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.password = passwordHash;
        this.createdAt = Instant.now();
        this.role = UserRole.USER;
    }

    public UUID getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public UserRole getRole() {
        return role;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    public String getAvatarContentType() {
        return avatarContentType;
    }

    public String getAvatarObjectKey() {
        return avatarObjectKey;
    }

    public void updateAvatarObject(String objectKey, String contentType) {
        this.avatar = null;
        this.avatarObjectKey = objectKey;
        this.avatarContentType = contentType;
    }
}
