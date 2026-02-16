package com.bntu.chinesecourses.model.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminMeResponse(
    Long id,
    String username,
    String role,
    Instant createdAt
) {
}
