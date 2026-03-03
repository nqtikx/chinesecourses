package com.bntu.chinesecourses.model.dto;

public record AdminUserListItemResponse(
    Long id,
    String username,
    String role,
    Long personId,
    String fullName
) {
}
