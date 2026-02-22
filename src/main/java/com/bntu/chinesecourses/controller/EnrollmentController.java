package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.exception.BadRequestException;
import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.EnrollmentCreateRequest;
import com.bntu.chinesecourses.model.dto.EnrollmentResponse;
import com.bntu.chinesecourses.model.dto.EnrollmentUpdateRequest;
import com.bntu.chinesecourses.model.entity.ChineseLevel;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import com.bntu.chinesecourses.service.CurrentUserService;
import com.bntu.chinesecourses.service.EnrollmentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

  private final EnrollmentService enrollmentService;
  private final CurrentUserService currentUserService;

  public EnrollmentController(EnrollmentService enrollmentService, CurrentUserService currentUserService) {
    this.enrollmentService = enrollmentService;
    this.currentUserService = currentUserService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public EnrollmentResponse create(@RequestBody EnrollmentCreateRequest request) {
    return enrollmentService.create(request);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public EnrollmentResponse get(@PathVariable Long id) {
    return enrollmentService.get(id, currentUserService.getCurrentTeacherId().orElse(null));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public EnrollmentResponse update(@PathVariable Long id, @RequestBody EnrollmentUpdateRequest request) {
    return enrollmentService.update(id, request);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public List<EnrollmentResponse> findTop50(
      @RequestParam(required = false) Long semesterId,
      @RequestParam(required = false) EnrollmentStatus status,
      @RequestParam(required = false) ChineseLevel level,
      @RequestParam(required = false) Long groupId
  ) {
    if (groupId != null) {
      if (semesterId != null || status != null || level != null) {
        throw new BadRequestException("Use either groupId or semesterId+status+level, not both");
      }
      return enrollmentService.findTop50ByGroupId(groupId, currentUserService.getCurrentTeacherId().orElse(null));
    }
    if (semesterId == null || status == null || level == null) {
      throw new BadRequestException("Specify groupId or semesterId, status, and level");
    }
    if (!currentUserService.isAdmin()) {
      throw new org.springframework.security.access.AccessDeniedException("Only admin can list enrollments by semester");
    }
    return enrollmentService.findTop50(semesterId, status, level);
  }

  @PatchMapping("/{id}/archive")
  @PreAuthorize("hasRole('ADMIN')")
  public EnrollmentResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return enrollmentService.setArchived(id, request.archived());
  }
}
