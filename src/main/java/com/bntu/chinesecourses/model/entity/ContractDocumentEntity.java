package com.bntu.chinesecourses.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "contract_document")
public class ContractDocumentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "enrollment_id", nullable = false)
  private Long enrollmentId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "course_id", nullable = false)
  private Long courseId;

  @Column(name = "group_id")
  private Long groupId;

  @Column(name = "contract_number", nullable = false, length = 64)
  private String contractNumber;

  @Column(name = "file_name", nullable = false, length = 256)
  private String fileName;

  @Column(name = "storage_path", nullable = false, length = 512)
  private String storagePath;

  @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal basePrice;

  @Column(name = "discount_percent", nullable = false)
  private Integer discountPercent;

  @Column(name = "final_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal finalPrice;

  @Column(name = "generated_by_user_id", nullable = false)
  private Long generatedByUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ContractDocumentEntity() {
  }

  public ContractDocumentEntity(
      Long id,
      Long enrollmentId,
      Long userId,
      Long courseId,
      Long groupId,
      String contractNumber,
      String fileName,
      String storagePath,
      BigDecimal basePrice,
      Integer discountPercent,
      BigDecimal finalPrice,
      Long generatedByUserId,
      Instant createdAt
  ) {
    this.id = id;
    this.enrollmentId = enrollmentId;
    this.userId = userId;
    this.courseId = courseId;
    this.groupId = groupId;
    this.contractNumber = contractNumber;
    this.fileName = fileName;
    this.storagePath = storagePath;
    this.basePrice = basePrice;
    this.discountPercent = discountPercent;
    this.finalPrice = finalPrice;
    this.generatedByUserId = generatedByUserId;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public Long getGroupId() {
    return groupId;
  }

  public String getContractNumber() {
    return contractNumber;
  }

  public String getFileName() {
    return fileName;
  }

  public String getStoragePath() {
    return storagePath;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public Integer getDiscountPercent() {
    return discountPercent;
  }

  public BigDecimal getFinalPrice() {
    return finalPrice;
  }

  public Long getGeneratedByUserId() {
    return generatedByUserId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
