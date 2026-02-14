package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;

public record PersonUpdateRequest(
    String lastName,
    String firstName,
    String middleName,
    LocalDate birthDate,
    String phone,
    String email,
    boolean archived
) {
}
