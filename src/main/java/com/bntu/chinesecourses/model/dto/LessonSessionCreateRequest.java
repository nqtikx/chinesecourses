package com.bntu.chinesecourses.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record LessonSessionCreateRequest(
    @NotNull Long groupId,
    Long teacherId,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    Instant actualStartsAt,
    Instant actualEndsAt,
    String topic,
    String room
) {
}
