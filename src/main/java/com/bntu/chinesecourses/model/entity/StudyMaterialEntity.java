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
@Table(name = "study_material")
public class StudyMaterialEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id", nullable = false)
  private StudyGroupEntity group;

  @Column(name = "file_name", nullable = false, length = 256)
  private String fileName;

  @Column(name = "file_type", length = 128)
  private String fileType;

  @Column(name = "storage_path", nullable = false, length = 512)
  private String storagePath;

  @Column(name = "uploader_user_id", nullable = false)
  private Long uploaderUserId;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected StudyMaterialEntity() {
  }

  public StudyMaterialEntity(
      Long id,
      StudyGroupEntity group,
      String fileName,
      String fileType,
      String storagePath,
      Long uploaderUserId,
      boolean archived,
      Instant createdAt
  ) {
    this.id = id;
    this.group = group;
    this.fileName = fileName;
    this.fileType = fileType;
    this.storagePath = storagePath;
    this.uploaderUserId = uploaderUserId;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public StudyGroupEntity getGroup() {
    return group;
  }

  public String getFileName() {
    return fileName;
  }

  public String getFileType() {
    return fileType;
  }

  public String getStoragePath() {
    return storagePath;
  }

  public Long getUploaderUserId() {
    return uploaderUserId;
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

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public void setStoragePath(String storagePath) {
    this.storagePath = storagePath;
  }

  public void setUploaderUserId(Long uploaderUserId) {
    this.uploaderUserId = uploaderUserId;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
