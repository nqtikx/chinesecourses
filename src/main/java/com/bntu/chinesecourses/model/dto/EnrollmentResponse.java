package com.bntu.chinesecourses.model.dto;

import com.bntu.chinesecourses.model.entity.ChineseLevel;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import java.time.Instant;
import java.time.LocalDate;

public record EnrollmentResponse(
    Long id,
    Long studentId,
    Long payerId,
    Long semesterId,
    Long groupId,
    EnrollmentStatus status,
    ChineseLevel level,
    boolean archived,
    LocalDate startDate,
    LocalDate endDate,
    String contractNumber,
    Instant createdAt
) {
}
