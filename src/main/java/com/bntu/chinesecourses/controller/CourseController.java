package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.CourseCreateRequest;
import com.bntu.chinesecourses.model.dto.CourseResponse;
import com.bntu.chinesecourses.model.dto.CourseUpdateRequest;
import com.bntu.chinesecourses.service.CourseService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

  private final CourseService courseService;

  public CourseController(CourseService courseService) {
    this.courseService = courseService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CourseResponse create(@RequestBody CourseCreateRequest request) {
    return courseService.create(request);
  }

  @GetMapping("/{id}")
  public CourseResponse get(@PathVariable Long id) {
    return courseService.get(id);
  }

  @GetMapping
  public List<CourseResponse> listTop50() {
    return courseService.listTop50();
  }

  @PutMapping("/{id}")
  public CourseResponse update(@PathVariable Long id, @RequestBody CourseUpdateRequest request) {
    return courseService.update(id, request);
  }

  @PatchMapping("/{id}/archive")
  public CourseResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return courseService.setArchived(id, request.archived());
  }
}
