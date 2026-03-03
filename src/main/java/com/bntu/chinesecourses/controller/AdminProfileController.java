package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.AdminUserListItemResponse;
import com.bntu.chinesecourses.model.dto.UserProfileResponse;
import com.bntu.chinesecourses.service.UserProfileService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProfileController {

  private final UserProfileService userProfileService;

  public AdminProfileController(UserProfileService userProfileService) {
    this.userProfileService = userProfileService;
  }

  @GetMapping
  public List<AdminUserListItemResponse> listUsers() {
    return userProfileService.listUsers();
  }

  @GetMapping("/{id}/profile")
  public UserProfileResponse profileById(@PathVariable Long id) {
    return userProfileService.getByUserId(id);
  }
}
