package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.EnrollmentCreateRequest;
import com.bntu.chinesecourses.model.dto.EnrollmentResponse;
import com.bntu.chinesecourses.model.dto.EnrollmentUpdateRequest;
import com.bntu.chinesecourses.model.entity.ChineseLevel;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import com.bntu.chinesecourses.service.EnrollmentService;
import java.util.List;
import org.springframework.http.HttpStatus;
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

  public EnrollmentController(EnrollmentService enrollmentService) {
    this.enrollmentService = enrollmentService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EnrollmentResponse create(@RequestBody EnrollmentCreateRequest request) {
    return enrollmentService.create(request);
  }

  @GetMapping("/{id}")
  public EnrollmentResponse get(@PathVariable Long id) {
    return enrollmentService.get(id);
  }

  @PutMapping("/{id}")
  public EnrollmentResponse update(@PathVariable Long id, @RequestBody EnrollmentUpdateRequest request) {
    return enrollmentService.update(id, request);
  }

  @GetMapping
  public List<EnrollmentResponse> findTop50(
      @RequestParam Long semesterId,
      @RequestParam EnrollmentStatus status,
      @RequestParam ChineseLevel level
  ) {
    return enrollmentService.findTop50(semesterId, status, level);
  }

  @PatchMapping("/{id}/archive")
  public EnrollmentResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return enrollmentService.setArchived(id, request.archived());
  }


}
