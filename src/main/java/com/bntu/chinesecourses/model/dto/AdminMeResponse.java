package com.bntu.chinesecourses.model.dto;

import java.time.Instant;

public record AdminMeResponse(
    Long id,
    String username,
    String role,
    Long teacherId,
    Instant createdAt
) {
}
