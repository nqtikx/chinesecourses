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
import java.time.LocalTime;

@Entity
@Table(name = "group_schedule_rule")
public class GroupScheduleRuleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id", nullable = false)
  private StudyGroupEntity group;

  @Column(name = "day_of_week", nullable = false)
  private short dayOfWeek;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(name = "room", length = 64)
  private String room;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private Instant createdAt;

  protected GroupScheduleRuleEntity() {
  }

  public GroupScheduleRuleEntity(
      Long id,
      StudyGroupEntity group,
      short dayOfWeek,
      LocalTime startTime,
      LocalTime endTime,
      String room,
      boolean active,
      boolean archived,
      Instant createdAt
  ) {
    this.id = id;
    this.group = group;
    this.dayOfWeek = dayOfWeek;
    this.startTime = startTime;
    this.endTime = endTime;
    this.room = room;
    this.active = active;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public StudyGroupEntity getGroup() {
    return group;
  }

  public short getDayOfWeek() {
    return dayOfWeek;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public String getRoom() {
    return room;
  }

  public boolean isActive() {
    return active;
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

  public void setDayOfWeek(short dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  public void setRoom(String room) {
    this.room = room;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
