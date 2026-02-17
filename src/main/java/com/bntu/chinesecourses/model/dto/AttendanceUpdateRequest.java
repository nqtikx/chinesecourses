package com.bntu.chinesecourses.model.dto;

import com.bntu.chinesecourses.model.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceUpdateRequest(
    @NotNull AttendanceStatus status,
    String comment,
    boolean archived
) {
}
