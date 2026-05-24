package com.bntu.chinesecourses.model.dto;

import java.time.Instant;

public record CourseResponse(
    Long id,
    String name,
    String description,
    boolean archived,
    Instant createdAt
) {
}
