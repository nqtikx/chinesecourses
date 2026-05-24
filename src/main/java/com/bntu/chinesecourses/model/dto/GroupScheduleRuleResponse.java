package com.bntu.chinesecourses.model.dto;

import java.time.Instant;
import java.time.LocalTime;

public record GroupScheduleRuleResponse(
    Long id,
    Long groupId,
    short dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    String room,
    boolean active,
    boolean archived,
    Instant createdAt
) {
}
