package com.bntu.chinesecourses.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "teacher")
public class TeacherEntity {

  @Id
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "id", nullable = false)
  private PersonEntity person;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected TeacherEntity() {
  }

  public TeacherEntity(Long id, PersonEntity person, boolean archived, Instant createdAt) {
    this.id = id;
    this.person = person;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public PersonEntity getPerson() {
    return person;
  }

  public boolean isArchived() {
    return archived;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setPerson(PersonEntity person) {
    this.person = person;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
