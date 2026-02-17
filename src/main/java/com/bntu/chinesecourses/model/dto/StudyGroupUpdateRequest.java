package com.bntu.chinesecourses.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudyGroupUpdateRequest(
    @NotNull
    Long semesterId,
    Long teacherId,
    @NotBlank
    String name,
    String scheduleNotes,
    boolean archived
) {
}
