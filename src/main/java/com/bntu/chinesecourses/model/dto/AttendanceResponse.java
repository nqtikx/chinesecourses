package com.bntu.chinesecourses.model.dto;

import java.time.Instant;

public record AttendanceResponse(
    Long id,
    Long lessonSessionId,
    Long enrollmentId,
    String status,
    String comment,
    Instant markedAt,
    Instant createdAt
) {
}
