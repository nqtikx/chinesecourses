package com.bntu.chinesecourses.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "semester")
public class SemesterEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "course_id", nullable = false)
  private Long courseId;

  @Column(name = "name", nullable = false, length = 256)
  private String name;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected SemesterEntity() {
  }

  public SemesterEntity(
      Long id,
      Long courseId,
      String name,
      LocalDate startDate,
      LocalDate endDate,
      boolean archived,
      Instant createdAt
  ) {
    this.id = id;
    this.courseId = courseId;
    this.name = name;
    this.startDate = startDate;
    this.endDate = endDate;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getCourseId() {
    return courseId;
  }

  public String getName() {
    return name;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public boolean isArchived() {
    return archived;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
