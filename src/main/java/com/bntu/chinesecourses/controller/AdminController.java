package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.AdminMeResponse;
import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import com.bntu.chinesecourses.service.AdminUserService;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final AdminUserService adminUserService;

  public AdminController(AdminUserService adminUserService) {
    this.adminUserService = adminUserService;
  }

  @GetMapping("/me")
  public ResponseEntity<AdminMeResponse> me(Principal principal) {
    AdminUserEntity user = adminUserService.findByUsername(principal.getName())
        .orElseThrow(() -> new NotFoundException("Admin user not found"));

    return ResponseEntity.ok(new AdminMeResponse(
        user.getId(),
        user.getUsername(),
        user.getRole().name(),
        user.getTeacherId(),
        user.getCreatedAt()
    ));
  }
}
