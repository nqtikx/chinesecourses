package com.bntu.chinesecourses.model.dto;

import com.bntu.chinesecourses.model.entity.AttendanceStatus;
import java.time.Instant;

public record AttendanceJournalCellResponse(
    Long lessonSessionId,
    AttendanceStatus status,
    String comment,
    Instant markedAt
) {
}
