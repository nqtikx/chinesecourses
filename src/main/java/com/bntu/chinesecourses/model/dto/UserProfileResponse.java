package com.bntu.chinesecourses.model.dto;

import java.util.List;

public record UserProfileResponse(
    Long userId,
    String username,
    String role,
    Long personId,
    String fullName,
    String email,
    String phone,
    String residentialAddress,
    String documentType,
    String documentSeries,
    String documentNumber,
    java.time.LocalDate documentIssueDate,
    String documentIssuedBy,
    String documentIdentificationNumber,
    java.util.List<PersonGuardianResponse> guardians,
    String teacherFullName,
    String teacherPhone,
    String teacherEmail,
    ProfileCourseItemResponse currentCourse,
    List<ProfileCourseItemResponse> completedCourses
) {
}
