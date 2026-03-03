package com.bntu.chinesecourses.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AdminEnrollmentPatchRequest(
    @NotNull String status,
    LocalDate endDate
) {
}
