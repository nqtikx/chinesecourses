package com.bntu.chinesecourses.model.dto;

public record CourseCreateRequest(
    String name,
    String description
) {
}
