package com.bntu.chinesecourses.model.dto;

import jakarta.validation.constraints.NotNull;

public record AdminContractCreateRequest(
    @NotNull Long userId,
    @NotNull Long courseId,
    Long groupId
) {
}
