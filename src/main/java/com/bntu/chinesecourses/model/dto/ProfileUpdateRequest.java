package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;

public record ProfileUpdateRequest(
    String firstName,
    String lastName,
    String middleName,
    LocalDate birthDate,
    String email,
    String phone,
    String residentialAddress,
    String documentType,
    String documentSeries,
    String documentNumber,
    LocalDate documentIssueDate,
    String documentIssuedBy,
    String documentIdentificationNumber,
    java.util.List<PersonGuardianUpdateRequest> guardians
) {
}
