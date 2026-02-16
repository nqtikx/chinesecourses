package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.SemesterCreateRequest;
import com.bntu.chinesecourses.model.dto.SemesterResponse;
import com.bntu.chinesecourses.model.dto.SemesterUpdateRequest;
import com.bntu.chinesecourses.service.SemesterService;
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
@RequestMapping("/api/semesters")
public class SemesterController {

  private final SemesterService semesterService;

  public SemesterController(SemesterService semesterService) {
    this.semesterService = semesterService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SemesterResponse create(@RequestBody SemesterCreateRequest request) {
    return semesterService.create(request);
  }

  @GetMapping("/{id}")
  public SemesterResponse get(@PathVariable Long id) {
    return semesterService.get(id);
  }

  @GetMapping
  public List<SemesterResponse> listTop50ByCourse(@RequestParam Long courseId) {
    return semesterService.listTop50ByCourse(courseId);
  }

  @PutMapping("/{id}")
  public SemesterResponse update(@PathVariable Long id, @RequestBody SemesterUpdateRequest request) {
    return semesterService.update(id, request);
  }

  @PatchMapping("/{id}/archive")
  public SemesterResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return semesterService.setArchived(id, request.archived());
  }
}
