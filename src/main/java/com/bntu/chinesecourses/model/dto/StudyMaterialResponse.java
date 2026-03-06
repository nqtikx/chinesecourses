package com.bntu.chinesecourses.model.dto;

import java.time.Instant;

public record StudyMaterialResponse(
    Long id,
    Long groupId,
    String fileName,
    String fileType,
    Long uploaderUserId,
    Instant createdAt
) {
}
