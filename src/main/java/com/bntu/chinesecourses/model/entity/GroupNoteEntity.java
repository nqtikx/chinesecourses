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
@Table(name = "group_note")
public class GroupNoteEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id", nullable = false)
  private StudyGroupEntity group;

  @Column(name = "author_user_id", nullable = false)
  private Long authorUserId;

  @Column(name = "text", nullable = false, columnDefinition = "text")
  private String text;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected GroupNoteEntity() {
  }

  public GroupNoteEntity(Long id, StudyGroupEntity group, Long authorUserId, String text, boolean archived, Instant createdAt) {
    this.id = id;
    this.group = group;
    this.authorUserId = authorUserId;
    this.text = text;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public StudyGroupEntity getGroup() {
    return group;
  }

  public Long getAuthorUserId() {
    return authorUserId;
  }

  public String getText() {
    return text;
  }

  public boolean isArchived() {
    return archived;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
