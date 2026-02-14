package com.bntu.chinesecourses.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PersonResponse(
    UUID id,
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
