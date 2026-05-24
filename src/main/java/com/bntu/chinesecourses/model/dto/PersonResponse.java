package com.bntu.chinesecourses.model.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PersonResponse(
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
    java.util.List<PersonGuardianResponse> guardians,
    boolean archived,
    Instant createdAt
) {
}
