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
    boolean archived,
    Instant createdAt
) {
}
