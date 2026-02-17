package com.bntu.chinesecourses.model.dto;

import java.time.Instant;

public record StudyGroupResponse(
    Long id,
    Long semesterId,
    Long teacherId,
    String name,
    String scheduleNotes,
    boolean archived,
    Instant createdAt
) {
}
