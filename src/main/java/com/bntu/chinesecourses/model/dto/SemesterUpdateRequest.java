package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;

public record SemesterUpdateRequest(
    Long courseId,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    boolean archived
) {
}
