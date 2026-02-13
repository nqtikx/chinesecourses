package com.bntu.chinesecourses.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_user")
public class AdminUserEntity {

  @Id
  private UUID id;

  @Column(name = "username", nullable = false, unique = true, length = 128)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 128)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 64)
  private AdminRole role;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected AdminUserEntity() {
  }

  public AdminUserEntity(UUID id, String username, String passwordHash, AdminRole role, Instant createdAt) {
    this.id = id;
    this.username = username;
    this.passwordHash = passwordHash;
    this.role = role;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public AdminRole getRole() {
    return role;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
