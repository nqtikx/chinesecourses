package com.bntu.chinesecourses.model.dto;

import java.time.Instant;

public record TeacherResponse(
    Long id,
    Long personId,
    boolean archived,
    Instant createdAt
) {
}
