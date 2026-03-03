package com.bntu.chinesecourses.model.dto;

import java.time.LocalDate;

public record ProfileCourseItemResponse(
    Long enrollmentId,
    Long courseId,
    String courseName,
    Long groupId,
    String groupName,
    String groupTeacherName,
    LocalDate startDate,
    LocalDate endDate,
    String status
) {
}
