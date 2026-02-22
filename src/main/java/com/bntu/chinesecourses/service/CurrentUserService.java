package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.entity.AdminRole;
import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

  private final AdminUserService adminUserService;

  public CurrentUserService(AdminUserService adminUserService) {
    this.adminUserService = adminUserService;
  }

  public Optional<AdminUserEntity> getCurrentUser() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .filter(Authentication::isAuthenticated)
        .map(Authentication::getName)
        .flatMap(adminUserService::findByUsername);
  }

  public boolean isAdmin() {
    return getCurrentUser()
        .map(u -> u.getRole() == AdminRole.ROLE_ADMIN)
        .orElse(false);
  }

  public boolean isTeacher() {
    return getCurrentUser()
        .map(u -> u.getRole() == AdminRole.ROLE_TEACHER)
        .orElse(false);
  }

  /** Non-null only when current user is a teacher. */
  public Optional<Long> getCurrentTeacherId() {
    return getCurrentUser()
        .filter(u -> u.getRole() == AdminRole.ROLE_TEACHER)
        .map(AdminUserEntity::getTeacherId)
        .filter(id -> id != null);
  }
}
