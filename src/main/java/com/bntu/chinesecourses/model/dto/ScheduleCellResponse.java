package com.bntu.chinesecourses.model.dto;

public record ScheduleCellResponse(
    Integer dayOfWeek,
    String dayLabel,
    String startTime,
    String endTime,
    String room
) {
}
