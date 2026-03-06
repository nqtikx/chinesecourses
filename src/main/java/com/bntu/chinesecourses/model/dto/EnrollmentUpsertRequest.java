package com.bntu.chinesecourses.model.dto;

import com.bntu.chinesecourses.model.entity.ChineseLevel;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record EnrollmentUpsertRequest(
    @NotNull Long studentId,
    Long payerId,
    @NotNull Long semesterId,
    Long groupId,
    @NotNull EnrollmentStatus status,
    @NotNull ChineseLevel level
) {
}
