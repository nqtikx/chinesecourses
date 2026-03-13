package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;

public record AttendanceJournalLessonResponse(
    Long lessonSessionId,
    LocalDate lessonDate,
    String topic,
    boolean canceled
) {
}
