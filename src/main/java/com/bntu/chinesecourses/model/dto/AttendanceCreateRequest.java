package com.bntu.chinesecourses.model.dto;

import com.bntu.chinesecourses.model.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceCreateRequest(
    @NotNull Long lessonSessionId,
    @NotNull Long enrollmentId,
    @NotNull AttendanceStatus status,
    String comment
) {
}
