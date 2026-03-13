package com.bntu.chinesecourses.model.dto;

public record PersonGuardianUpdateRequest(
    Long id,
    String fullName,
    String phone,
    String relationType,
    boolean primaryGuardian,
    boolean archived
) {
}
