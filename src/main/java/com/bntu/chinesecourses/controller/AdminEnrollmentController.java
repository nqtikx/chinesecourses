package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.AdminEnrollmentCreateRequest;
import com.bntu.chinesecourses.model.dto.AdminEnrollmentPatchRequest;
import com.bntu.chinesecourses.model.dto.EnrollmentResponse;
import com.bntu.chinesecourses.service.AdminEnrollmentManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/enrollments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEnrollmentController {

  private final AdminEnrollmentManagementService adminEnrollmentManagementService;

  public AdminEnrollmentController(AdminEnrollmentManagementService adminEnrollmentManagementService) {
    this.adminEnrollmentManagementService = adminEnrollmentManagementService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EnrollmentResponse createInProgress(@Valid @RequestBody AdminEnrollmentCreateRequest request) {
    return adminEnrollmentManagementService.assignInProgress(request);
  }

  @PatchMapping("/{id}")
  public EnrollmentResponse patchStatus(@PathVariable Long id, @Valid @RequestBody AdminEnrollmentPatchRequest request) {
    return adminEnrollmentManagementService.patchStatus(id, request);
  }
}
