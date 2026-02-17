package com.bntu.chinesecourses.model.dto;

import com.bntu.chinesecourses.model.entity.AttendanceStatus;
import java.time.Instant;

public record AttendanceResponse(
    Long id,
    Long lessonSessionId,
    Long enrollmentId,
    AttendanceStatus status,
    String comment,
    Instant markedAt,
    boolean archived,
    Instant createdAt
) {
}
