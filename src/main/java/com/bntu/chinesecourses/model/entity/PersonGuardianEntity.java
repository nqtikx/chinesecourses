package com.bntu.chinesecourses.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "person_guardian")
public class PersonGuardianEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "child_person_id", nullable = false)
  private Long childPersonId;

  @Column(name = "full_name", nullable = false, length = 256)
  private String fullName;

  @Column(name = "phone", length = 32)
  private String phone;

  @Column(name = "relation_type", length = 64)
  private String relationType;

  @Column(name = "primary_guardian", nullable = false)
  private boolean primaryGuardian;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
  private Instant createdAt;

  protected PersonGuardianEntity() {
  }

  public PersonGuardianEntity(
      Long id,
      Long childPersonId,
      String fullName,
      String phone,
      String relationType,
      boolean primaryGuardian,
      boolean archived,
      Instant createdAt) {
    this.id = id;
    this.childPersonId = childPersonId;
    this.fullName = fullName;
    this.phone = phone;
    this.relationType = relationType;
    this.primaryGuardian = primaryGuardian;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getChildPersonId() {
    return childPersonId;
  }

  public String getFullName() {
    return fullName;
  }

  public String getPhone() {
    return phone;
  }

  public String getRelationType() {
    return relationType;
  }

  public boolean isPrimaryGuardian() {
    return primaryGuardian;
  }

  public boolean isArchived() {
    return archived;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setChildPersonId(Long childPersonId) {
    this.childPersonId = childPersonId;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public void setRelationType(String relationType) {
    this.relationType = relationType;
  }

  public void setPrimaryGuardian(boolean primaryGuardian) {
    this.primaryGuardian = primaryGuardian;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
