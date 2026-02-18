package com.bntu.chinesecourses.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_user")
public class AdminUserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", nullable = false, length = 128, unique = true)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 128)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 64)
  private UserRole role;

  @Column(name = "person_id")
  private Long personId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AdminUserEntity() {}

  public AdminUserEntity(
      Long id,
      String username,
      String passwordHash,
      UserRole role,
      Long personId,
      Instant createdAt
  ) {
    this.id = id;
    this.username = username;
    this.passwordHash = passwordHash;
    this.role = role;
    this.personId = personId;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public UserRole getRole() {
    return role;
  }

  public Long getPersonId() {
    return personId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public void setPersonId(Long personId) {
    this.personId = personId;
  }
}
