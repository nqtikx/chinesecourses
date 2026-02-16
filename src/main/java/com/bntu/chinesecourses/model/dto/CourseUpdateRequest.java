package com.bntu.chinesecourses.model.dto;

public record CourseUpdateRequest(
    String name,
    String description,
    boolean archived
) {
}
