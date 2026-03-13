package com.bntu.chinesecourses.model.dto;

import java.time.Instant;

public record LessonSessionStatusPatchRequest(
    boolean canceled,
    Instant actualStartsAt,
    Instant actualEndsAt
) {
}
