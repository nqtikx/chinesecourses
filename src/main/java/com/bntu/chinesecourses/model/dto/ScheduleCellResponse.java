package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;

public record ScheduleCellResponse(
    Integer dayOfWeek,
    LocalDate date,
    String dayLabel,
    String startTime,
    String endTime,
    String room,
    String lessonType,
    boolean holiday,
    String holidayTitle
) {
}
