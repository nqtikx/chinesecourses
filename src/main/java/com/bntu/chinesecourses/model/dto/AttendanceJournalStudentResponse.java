package com.bntu.chinesecourses.model.dto;

import java.util.List;

public record AttendanceJournalStudentResponse(
    Long enrollmentId,
    Long studentId,
    String studentFullName,
    List<AttendanceJournalCellResponse> attendance
) {
}
