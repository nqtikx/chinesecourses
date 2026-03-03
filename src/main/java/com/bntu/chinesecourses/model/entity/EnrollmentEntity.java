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
import java.time.LocalDate;

@Entity
@Table(name = "enrollment")
public class EnrollmentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Column(name = "payer_id")
  private Long payerId;

  @Column(name = "semester_id", nullable = false)
  private Long semesterId;

  @Column(name = "group_id")
  private Long groupId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private EnrollmentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "level", nullable = false, length = 32)
  private ChineseLevel level;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "contract_number", length = 64)
  private String contractNumber;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected EnrollmentEntity() {
  }

  public EnrollmentEntity(
      Long id,
      Long studentId,
      Long payerId,
      Long semesterId,
      Long groupId,
      EnrollmentStatus status,
      ChineseLevel level,
      boolean archived,
      LocalDate startDate,
      LocalDate endDate,
      String contractNumber,
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
    this.startDate = startDate;
    this.endDate = endDate;
    this.contractNumber = contractNumber;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getStudentId() {
    return studentId;
  }

  public Long getPayerId() {
    return payerId;
  }

  public Long getSemesterId() {
    return semesterId;
  }

  public Long getGroupId() {
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

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public String getContractNumber() {
    return contractNumber;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setPayerId(Long payerId) {
    this.payerId = payerId;
  }

  public void setSemesterId(Long semesterId) {
    this.semesterId = semesterId;
  }

  public void setGroupId(Long groupId) {
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

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public void setContractNumber(String contractNumber) {
    this.contractNumber = contractNumber;
  }
}
