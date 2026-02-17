package com.bntu.chinesecourses.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudyGroupCreateRequest(
    @NotNull
    Long semesterId,
    Long teacherId,
    @NotBlank
    String name,
    String scheduleNotes
) {
}
