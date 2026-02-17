package com.bntu.chinesecourses.model.dto;

import java.time.Instant;

public record LessonSessionResponse(
    Long id,
    Long groupId,
    Long teacherId,
    Instant startsAt,
    Instant endsAt,
    String topic,
    String room,
    boolean canceled,
    boolean archived,
    Instant createdAt
) {
}
