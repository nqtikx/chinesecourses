package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.LessonSessionCreateRequest;
import com.bntu.chinesecourses.model.dto.LessonSessionResponse;
import com.bntu.chinesecourses.model.dto.LessonSessionStatusPatchRequest;
import com.bntu.chinesecourses.model.dto.LessonSessionUpdateRequest;
import com.bntu.chinesecourses.service.CurrentUserService;
import com.bntu.chinesecourses.service.LessonSessionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@RequestMapping("/api/lesson-sessions")
public class LessonSessionController {

  private final LessonSessionService lessonSessionService;
  private final CurrentUserService currentUserService;

  public LessonSessionController(LessonSessionService lessonSessionService, CurrentUserService currentUserService) {
    this.lessonSessionService = lessonSessionService;
    this.currentUserService = currentUserService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public LessonSessionResponse create(@Valid @RequestBody LessonSessionCreateRequest request) {
    return lessonSessionService.create(request);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'GROUP')")
  public LessonSessionResponse get(@PathVariable Long id) {
    LessonSessionResponse response = lessonSessionService.get(id, currentUserService.getCurrentTeacherId().orElse(null));
    if (currentUserService.isGroupAccount()) {
      Long currentGroupId = currentUserService.getCurrentGroupId()
          .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Group id is not linked"));
      if (!currentGroupId.equals(response.groupId())) {
        throw new org.springframework.security.access.AccessDeniedException("Access only to own group sessions");
      }
    }
    return response;
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public LessonSessionResponse update(@PathVariable Long id, @Valid @RequestBody LessonSessionUpdateRequest request) {
    return lessonSessionService.update(id, request);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'GROUP')")
  public List<LessonSessionResponse> findTop50(@RequestParam Long groupId) {
    if (currentUserService.isGroupAccount()) {
      Long currentGroupId = currentUserService.getCurrentGroupId()
          .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Group id is not linked"));
      if (!currentGroupId.equals(groupId)) {
        throw new org.springframework.security.access.AccessDeniedException("Access only to own group sessions");
      }
    }
    return lessonSessionService.findTop50ByGroup(groupId, currentUserService.getCurrentTeacherId().orElse(null));
  }

  @PatchMapping("/{id}/archive")
  @PreAuthorize("hasRole('ADMIN')")
  public LessonSessionResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return lessonSessionService.setArchived(id, request.archived());
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  public LessonSessionResponse patchStatus(@PathVariable Long id, @RequestBody LessonSessionStatusPatchRequest request) {
    Long approverUserId = currentUserService.getCurrentUserId().orElse(null);
    return lessonSessionService.patchStatus(
        id,
        request,
        approverUserId,
        currentUserService.getCurrentTeacherId().orElse(null));
  }

  @GetMapping("/range")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER','GROUP')")
  public List<LessonSessionResponse> listByDateRange(
      @RequestParam Long groupId,
      @RequestParam LocalDate from,
      @RequestParam LocalDate to
  ) {
    if (currentUserService.isGroupAccount()) {
      Long currentGroupId = currentUserService.getCurrentGroupId()
          .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Group id is not linked"));
      if (!currentGroupId.equals(groupId)) {
        throw new org.springframework.security.access.AccessDeniedException("Access only to own group sessions");
      }
    }
    return lessonSessionService.findByGroupAndDateRange(groupId, from, to, currentUserService.getCurrentTeacherId().orElse(null));
  }
}
