package com.bntu.chinesecourses.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AcademicHolidayCreateRequest(
    @NotNull LocalDate holidayDate,
    @NotBlank String title,
    boolean noClasses
) {
}
