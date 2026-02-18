package com.bntu.chinesecourses.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "attendance")
public class AttendanceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lesson_session_id", nullable = false)
  private LessonSessionEntity lessonSession;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private EnrollmentEntity enrollment;

  @Column(name = "status", nullable = false, length = 16)
  private String status;

  @Column(name = "comment", length = 256)
  private String comment;

  @Column(name = "marked_at", nullable = false)
  private Instant markedAt;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AttendanceEntity() {
  }

  public AttendanceEntity(
      Long id,
      LessonSessionEntity lessonSession,
      EnrollmentEntity enrollment,
      String status,
      String comment,
      Instant markedAt,
      boolean archived,
      Instant createdAt
  ) {
    this.id = id;
    this.lessonSession = lessonSession;
    this.enrollment = enrollment;
    this.status = status;
    this.comment = comment;
    this.markedAt = markedAt;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (markedAt == null) {
      markedAt = now;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    if (markedAt == null) {
      markedAt = Instant.now();
    }
  }

  public Long getId() {
    return id;
  }

  public LessonSessionEntity getLessonSession() {
    return lessonSession;
  }

  public EnrollmentEntity getEnrollment() {
    return enrollment;
  }

  public String getStatus() {
    return status;
  }

  public String getComment() {
    return comment;
  }

  public Instant getMarkedAt() {
    return markedAt;
  }

  public boolean isArchived() {
    return archived;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setLessonSession(LessonSessionEntity lessonSession) {
    this.lessonSession = lessonSession;
  }

  public void setEnrollment(EnrollmentEntity enrollment) {
    this.enrollment = enrollment;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void setMarkedAt(Instant markedAt) {
    this.markedAt = markedAt;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
