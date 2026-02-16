package com.bntu.chinesecourses.model.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SemesterResponse(
    Long id,
    Long courseId,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    boolean archived,
    Instant createdAt
) {
}
