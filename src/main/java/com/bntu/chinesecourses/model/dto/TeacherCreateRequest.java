package com.bntu.chinesecourses.model.dto;

import jakarta.validation.constraints.NotNull;

public record TeacherCreateRequest(
    @NotNull
    Long personId
) {
}
