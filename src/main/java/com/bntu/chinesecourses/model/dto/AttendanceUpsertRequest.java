package com.bntu.chinesecourses.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttendanceUpsertRequest(
    @NotNull Long enrollmentId,
    @NotBlank @Size(max = 16) String status,
    @Size(max = 256) String comment
) {
}
