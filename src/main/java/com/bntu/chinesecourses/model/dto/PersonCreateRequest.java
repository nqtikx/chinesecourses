package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;

public record PersonCreateRequest(
    String lastName,
    String firstName,
    String middleName,
    LocalDate birthDate,
    String phone,
    String email
) {
}
