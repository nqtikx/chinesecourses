package com.bntu.chinesecourses.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AdminEnrollmentCreateRequest(
    @NotNull Long userId,
    @NotNull Long courseId,
    Long groupId,
    LocalDate startDate
) {
}
