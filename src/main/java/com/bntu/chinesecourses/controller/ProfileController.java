package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.UserProfileResponse;
import com.bntu.chinesecourses.model.dto.ProfileUpdateRequest;
import com.bntu.chinesecourses.service.UserProfileService;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

  private final UserProfileService userProfileService;

  public ProfileController(UserProfileService userProfileService) {
    this.userProfileService = userProfileService;
  }

  @GetMapping("/me")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER','USER')")
  public UserProfileResponse me(Principal principal) {
    return userProfileService.getMyProfile(principal.getName());
  }

  @PatchMapping("/me")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER','USER')")
  public UserProfileResponse updateMe(Principal principal, @RequestBody ProfileUpdateRequest request) {
    return userProfileService.updateMyProfile(principal.getName(), request);
  }
}
