package com.bntu.chinesecourses.model.dto;

import jakarta.validation.constraints.NotBlank;

public record GroupNoteCreateRequest(
    @NotBlank String text
) {
}
