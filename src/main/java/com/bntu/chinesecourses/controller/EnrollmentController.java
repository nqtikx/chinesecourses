package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.EnrollmentResponse;
import com.bntu.chinesecourses.model.dto.EnrollmentUpsertRequest;
import com.bntu.chinesecourses.service.EnrollmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

  private final EnrollmentService enrollmentService;

  public EnrollmentController(EnrollmentService enrollmentService) {
    this.enrollmentService = enrollmentService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public EnrollmentResponse create(@RequestBody @Valid EnrollmentUpsertRequest request) {
    return enrollmentService.create(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public EnrollmentResponse update(@PathVariable Long id, @RequestBody @Valid EnrollmentUpsertRequest request) {
    return enrollmentService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void archive(@PathVariable Long id) {
    enrollmentService.archive(id);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
  public EnrollmentResponse get(@PathVariable Long id) {
    return enrollmentService.get(id);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
  public List<EnrollmentResponse> list(
      @RequestParam(value = "semesterId", required = false) Long semesterId,
      @RequestParam(value = "groupId", required = false) Long groupId
  ) {
    if (semesterId != null) {
      return enrollmentService.listBySemester(semesterId);
    }
    if (groupId != null) {
      return enrollmentService.listByGroup(groupId);
    }
    throw new IllegalArgumentException("semesterId or groupId is required");
  }
}
