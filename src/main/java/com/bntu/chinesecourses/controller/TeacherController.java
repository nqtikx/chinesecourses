package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.TeacherCreateRequest;
import com.bntu.chinesecourses.model.dto.TeacherResponse;
import com.bntu.chinesecourses.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

  private final TeacherService teacherService;

  public TeacherController(TeacherService teacherService) {
    this.teacherService = teacherService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public TeacherResponse create(@Valid @RequestBody TeacherCreateRequest request) {
    return teacherService.create(request);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public TeacherResponse get(@PathVariable Long id) {
    return teacherService.get(id);
  }

  @PatchMapping("/{id}/archive")
  @PreAuthorize("hasRole('ADMIN')")
  public TeacherResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return teacherService.setArchived(id, request.archived());
  }
}
