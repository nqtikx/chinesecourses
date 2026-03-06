package com.bntu.chinesecourses.model.dto;

public record ClassProfileGroupItemResponse(
    Long groupId,
    String groupName,
    String courseName,
    String semesterName,
    String teacherName,
    long studentsCount
) {
}
