package com.bntu.chinesecourses.model.dto;

import java.util.List;

public record GroupScheduleTableResponse(
    Long groupId,
    String groupName,
    String courseName,
    String semesterName,
    String teacherName,
    List<ScheduleCellResponse> rows
) {
}
