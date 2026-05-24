package com.bntu.chinesecourses.model.dto;

import java.time.Instant;

public record TeacherResponse(
    Long id,
    Long personId,
    String fullName,
    String phone,
    String email,
    boolean archived,
    Instant createdAt
) {
}
