package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.LessonSessionCreateRequest;
import com.bntu.chinesecourses.model.dto.LessonSessionResponse;
import com.bntu.chinesecourses.model.dto.LessonSessionUpdateRequest;
import com.bntu.chinesecourses.service.LessonSessionService;
import jakarta.validation.Valid;
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
@RequestMapping("/lesson-sessions")
public class LessonSessionController {

  private final LessonSessionService lessonSessionService;

  public LessonSessionController(LessonSessionService lessonSessionService) {
    this.lessonSessionService = lessonSessionService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public LessonSessionResponse create(@Valid @RequestBody LessonSessionCreateRequest request) {
    return lessonSessionService.create(request);
  }

  @GetMapping("/{id}")
  public LessonSessionResponse get(@PathVariable Long id) {
    return lessonSessionService.get(id);
  }

  @PutMapping("/{id}")
  public LessonSessionResponse update(@PathVariable Long id, @Valid @RequestBody LessonSessionUpdateRequest request) {
    return lessonSessionService.update(id, request);
  }

  @GetMapping
  public List<LessonSessionResponse> findTop50(@RequestParam Long groupId) {
    return lessonSessionService.findTop50ByGroup(groupId);
  }

  @PatchMapping("/{id}/archive")
  public LessonSessionResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return lessonSessionService.setArchived(id, request.archived());
  }
}
