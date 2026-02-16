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

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
