package com.bntu.chinesecourses.model.dto;

import java.time.Instant;

public record PersonGuardianResponse(
    Long id,
    Long childPersonId,
    String fullName,
    String phone,
    String relationType,
    boolean primaryGuardian,
    boolean archived,
    Instant createdAt
) {
}
