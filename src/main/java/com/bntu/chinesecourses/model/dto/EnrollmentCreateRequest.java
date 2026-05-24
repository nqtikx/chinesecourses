package com.bntu.chinesecourses.model.dto;

import com.bntu.chinesecourses.model.entity.ChineseLevel;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import java.time.LocalDate;

public record EnrollmentCreateRequest(
    Long studentId,
    Long payerId,
    Long semesterId,
    Long groupId,
    EnrollmentStatus status,
    ChineseLevel level,
    LocalDate startDate
) {
}
