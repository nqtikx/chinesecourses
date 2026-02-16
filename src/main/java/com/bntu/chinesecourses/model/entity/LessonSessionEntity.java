package com.bntu.chinesecourses.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "lesson_session")
public class LessonSessionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id", nullable = false)
  private StudyGroupEntity group;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id")
  private TeacherEntity teacher;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @Column(name = "topic", length = 256)
  private String topic;

  @Column(name = "room", length = 64)
  private String room;

  @Column(name = "canceled", nullable = false)
  private boolean canceled;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected LessonSessionEntity() {
  }

  public LessonSessionEntity(
      Long id,
      StudyGroupEntity group,
      TeacherEntity teacher,
      Instant startsAt,
      Instant endsAt,
      String topic,
      String room,
      boolean canceled,
      boolean archived,
      Instant createdAt
  ) {
    this.id = id;
    this.group = group;
    this.teacher = teacher;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.topic = topic;
    this.room = room;
    this.canceled = canceled;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public StudyGroupEntity getGroup() {
    return group;
  }

  public TeacherEntity getTeacher() {
    return teacher;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public Instant getEndsAt() {
    return endsAt;
  }

  public String getTopic() {
    return topic;
  }

  public String getRoom() {
    return room;
  }

  public boolean isCanceled() {
    return canceled;
  }

  public boolean isArchived() {
    return archived;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setGroup(StudyGroupEntity group) {
    this.group = group;
  }

  public void setTeacher(TeacherEntity teacher) {
    this.teacher = teacher;
  }

  public void setStartsAt(Instant startsAt) {
    this.startsAt = startsAt;
  }

  public void setEndsAt(Instant endsAt) {
    this.endsAt = endsAt;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public void setRoom(String room) {
    this.room = room;
  }

  public void setCanceled(boolean canceled) {
    this.canceled = canceled;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
