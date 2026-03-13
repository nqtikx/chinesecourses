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
@Table(name = "person")
public class PersonEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "last_name", nullable = false, length = 128)
  private String lastName;

  @Column(name = "first_name", nullable = false, length = 128)
  private String firstName;

  @Column(name = "middle_name", length = 128)
  private String middleName;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "phone", length = 32)
  private String phone;

  @Column(name = "email", length = 256)
  private String email;

  @Column(name = "residential_address", length = 512)
  private String residentialAddress;

  @Column(name = "document_type", length = 128)
  private String documentType;

  @Column(name = "document_series", length = 64)
  private String documentSeries;

  @Column(name = "document_number", length = 64)
  private String documentNumber;

  @Column(name = "document_issue_date")
  private LocalDate documentIssueDate;

  @Column(name = "document_issued_by", length = 512)
  private String documentIssuedBy;

  @Column(name = "document_identification_number", length = 128)
  private String documentIdentificationNumber;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected PersonEntity() {
  }

  public PersonEntity(
      Long id,
      String lastName,
      String firstName,
      String middleName,
      LocalDate birthDate,
      String phone,
      String email,
      String residentialAddress,
      String documentType,
      String documentSeries,
      String documentNumber,
      LocalDate documentIssueDate,
      String documentIssuedBy,
      String documentIdentificationNumber,
      boolean archived,
      Instant createdAt
  ) {
    this.id = id;
    this.lastName = lastName;
    this.firstName = firstName;
    this.middleName = middleName;
    this.birthDate = birthDate;
    this.phone = phone;
    this.email = email;
    this.residentialAddress = residentialAddress;
    this.documentType = documentType;
    this.documentSeries = documentSeries;
    this.documentNumber = documentNumber;
    this.documentIssueDate = documentIssueDate;
    this.documentIssuedBy = documentIssuedBy;
    this.documentIdentificationNumber = documentIdentificationNumber;
    this.archived = archived;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getLastName() {
    return lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getMiddleName() {
    return middleName;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  public String getResidentialAddress() {
    return residentialAddress;
  }

  public String getDocumentType() {
    return documentType;
  }

  public String getDocumentSeries() {
    return documentSeries;
  }

  public String getDocumentNumber() {
    return documentNumber;
  }

  public LocalDate getDocumentIssueDate() {
    return documentIssueDate;
  }

  public String getDocumentIssuedBy() {
    return documentIssuedBy;
  }

  public String getDocumentIdentificationNumber() {
    return documentIdentificationNumber;
  }

  public boolean isArchived() {
    return archived;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setMiddleName(String middleName) {
    this.middleName = middleName;
  }

  public void setBirthDate(LocalDate birthDate) {
    this.birthDate = birthDate;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setResidentialAddress(String residentialAddress) {
    this.residentialAddress = residentialAddress;
  }

  public void setDocumentType(String documentType) {
    this.documentType = documentType;
  }

  public void setDocumentSeries(String documentSeries) {
    this.documentSeries = documentSeries;
  }

  public void setDocumentNumber(String documentNumber) {
    this.documentNumber = documentNumber;
  }

  public void setDocumentIssueDate(LocalDate documentIssueDate) {
    this.documentIssueDate = documentIssueDate;
  }

  public void setDocumentIssuedBy(String documentIssuedBy) {
    this.documentIssuedBy = documentIssuedBy;
  }

  public void setDocumentIdentificationNumber(String documentIdentificationNumber) {
    this.documentIdentificationNumber = documentIdentificationNumber;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
