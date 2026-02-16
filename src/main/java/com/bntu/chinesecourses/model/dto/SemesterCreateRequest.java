package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;

public record SemesterCreateRequest(
    Long courseId,
    String name,
    LocalDate startDate,
    LocalDate endDate
) {
}