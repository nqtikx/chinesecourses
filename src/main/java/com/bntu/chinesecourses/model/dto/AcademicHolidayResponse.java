package com.bntu.chinesecourses.model.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AcademicHolidayResponse(
    Long id,
    LocalDate holidayDate,
    String title,
    boolean noClasses,
    boolean archived,
    Instant createdAt
) {
}
