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
@Table(name = "academic_holiday")
public class AcademicHolidayEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "holiday_date", nullable = false)
  private LocalDate holidayDate;

  @Column(name = "title", nullable = false, length = 256)
  private String title;

  @Column(name = "no_classes", nullable = false)
  private boolean noClasses;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
  private Instant createdAt;

  protected AcademicHolidayEntity() {
  }

  public AcademicHolidayEntity(Long id, LocalDate holidayDate, String title, boolean noClasses, boolean archived, Instant createdAt) {
    this.id = id;
    this.holidayDate = holidayDate;
    this.title = title;
    this.noClasses = noClasses;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public LocalDate getHolidayDate() {
    return holidayDate;
  }

  public String getTitle() {
    return title;
  }

  public boolean isNoClasses() {
    return noClasses;
  }

  public boolean isArchived() {
    return archived;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setHolidayDate(LocalDate holidayDate) {
    this.holidayDate = holidayDate;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setNoClasses(boolean noClasses) {
    this.noClasses = noClasses;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
