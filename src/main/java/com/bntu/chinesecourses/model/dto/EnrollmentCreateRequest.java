package com.bntu.chinesecourses.model.dto;

import com.bntu.chinesecourses.model.entity.ChineseLevel;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import java.util.UUID;

public record EnrollmentCreateRequest(
    UUID studentId,
    UUID payerId,
    UUID semesterId,
    UUID groupId,
    EnrollmentStatus status,
    ChineseLevel level
) {
}
