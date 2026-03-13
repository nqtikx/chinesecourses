package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;
import java.util.List;

public record AttendanceJournalResponse(
    Long groupId,
    LocalDate fromDate,
    LocalDate toDate,
    List<AttendanceJournalLessonResponse> lessons,
    List<AttendanceJournalStudentResponse> students
) {
}
