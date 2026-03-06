package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;

public record ProfileUpdateRequest(
    String firstName,
    String lastName,
    String middleName,
    LocalDate birthDate,
    String email,
    String phone
) {
}
