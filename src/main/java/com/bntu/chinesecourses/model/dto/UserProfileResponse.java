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
    ProfileCourseItemResponse currentCourse,
    List<ProfileCourseItemResponse> completedCourses
) {
}
