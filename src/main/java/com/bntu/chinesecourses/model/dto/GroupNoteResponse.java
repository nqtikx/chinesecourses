package com.bntu.chinesecourses.model.dto;

import java.time.Instant;

public record GroupNoteResponse(
    Long id,
    Long groupId,
    Long authorUserId,
    String text,
    Instant createdAt
) {
}
