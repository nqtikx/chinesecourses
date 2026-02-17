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
@Table(name = "study_group")
public class StudyGroupEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "semester_id", nullable = false)
  private SemesterEntity semester;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id")
  private TeacherEntity teacher;

  @Column(name = "name", nullable = false, length = 256)
  private String name;

  @Column(name = "schedule_notes")
  private String scheduleNotes;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private Instant createdAt;

  protected StudyGroupEntity() {
  }

  public StudyGroupEntity(
      Long id,
      SemesterEntity semester,
      TeacherEntity teacher,
      String name,
      String scheduleNotes,
      boolean archived,
      Instant createdAt
  ) {
    this.id = id;
    this.semester = semester;
    this.teacher = teacher;
    this.name = name;
    this.scheduleNotes = scheduleNotes;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getSemesterId() {
    return semester != null ? semester.getId() : null;
  }

  public SemesterEntity getSemester() {
    return semester;
  }

  public TeacherEntity getTeacher() {
    return teacher;
  }

  public String getName() {
    return name;
  }

  public String getScheduleNotes() {
    return scheduleNotes;
  }

  public boolean isArchived() {
    return archived;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setSemester(SemesterEntity semester) {
    this.semester = semester;
  }

  public void setTeacher(TeacherEntity teacher) {
    this.teacher = teacher;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setScheduleNotes(String scheduleNotes) {
    this.scheduleNotes = scheduleNotes;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
