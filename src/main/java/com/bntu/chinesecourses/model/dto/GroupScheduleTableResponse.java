package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;
import java.util.List;

public record GroupScheduleTableResponse(
    Long groupId,
    String groupName,
    String courseName,
    String semesterName,
    String teacherName,
    LocalDate weekStart,
    List<ScheduleCellResponse> rows
) {
}
