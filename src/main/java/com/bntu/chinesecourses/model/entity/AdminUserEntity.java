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

  @Column(name = "username", nullable = false, unique = true, length = 128)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 128)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 64)
  private AdminRole role;

  @Column(name = "teacher_id")
  private Long teacherId;

  @Column(name = "person_id")
  private Long personId;

  @Column(name = "group_id")
  private Long groupId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AdminUserEntity() {
  }

  public AdminUserEntity(
      Long id,
      String username,
      String passwordHash,
      AdminRole role,
      Long teacherId,
      Long personId,
      Instant createdAt
  ) {
    this(id, username, passwordHash, role, teacherId, personId, null, createdAt);
  }

  public AdminUserEntity(
      Long id,
      String username,
      String passwordHash,
      AdminRole role,
      Long teacherId,
      Long personId,
      Long groupId,
      Instant createdAt
  ) {
    this.id = id;
    this.username = username;
    this.passwordHash = passwordHash;
    this.role = role;
    this.teacherId = teacherId;
    this.personId = personId;
    this.groupId = groupId;
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

  public AdminRole getRole() {
    return role;
  }

  public Long getTeacherId() {
    return teacherId;
  }

  public Long getPersonId() {
    return personId;
  }

  public Long getGroupId() {
    return groupId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
