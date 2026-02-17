package com.bntu.chinesecourses.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record GroupScheduleRuleUpdateRequest(
    @NotNull Long groupId,
    @Min(1) @Max(7) short dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    String room,
    boolean active,
    boolean archived
) {
}
