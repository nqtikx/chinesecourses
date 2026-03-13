package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;

public record PersonUpdateRequest(
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
    java.util.List<PersonGuardianUpdateRequest> guardians,
    boolean archived
) {
}
