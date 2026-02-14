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
@Table(name = "enrollment")
public class EnrollmentEntity {

  @Id
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "payer_id")
  private UUID payerId;

  @Column(name = "semester_id", nullable = false)
  private UUID semesterId;

  @Column(name = "group_id")
  private UUID groupId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private EnrollmentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "level", nullable = false, length = 32)
  private ChineseLevel level;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected EnrollmentEntity() {
  }

  public EnrollmentEntity(
      UUID id,
      UUID studentId,
      UUID payerId,
      UUID semesterId,
      UUID groupId,
      EnrollmentStatus status,
      ChineseLevel level,
      boolean archived,
      Instant createdAt
  ) {
    this.id = id;
    this.studentId = studentId;
    this.payerId = payerId;
    this.semesterId = semesterId;
    this.groupId = groupId;
    this.status = status;
    this.level = level;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public UUID getPayerId() {
    return payerId;
  }

  public UUID getSemesterId() {
    return semesterId;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public ChineseLevel getLevel() {
    return level;
  }

  public boolean isArchived() {
    return archived;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setPayerId(UUID payerId) {
    this.payerId = payerId;
  }

  public void setSemesterId(UUID semesterId) {
    this.semesterId = semesterId;
  }

  public void setGroupId(UUID groupId) {
    this.groupId = groupId;
  }

  public void setStatus(EnrollmentStatus status) {
    this.status = status;
  }

  public void setLevel(ChineseLevel level) {
    this.level = level;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
