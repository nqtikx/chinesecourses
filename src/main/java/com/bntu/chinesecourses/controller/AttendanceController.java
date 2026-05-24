package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.AttendanceCreateRequest;
import com.bntu.chinesecourses.model.dto.AttendanceJournalResponse;
import com.bntu.chinesecourses.model.dto.AttendanceResponse;
import com.bntu.chinesecourses.model.dto.AttendanceUpdateRequest;
import com.bntu.chinesecourses.service.AttendanceService;
import com.bntu.chinesecourses.service.CurrentUserService;
import com.bntu.chinesecourses.service.EnrollmentService;
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
@RequestMapping("/api/attendance")
public class AttendanceController {

  private final AttendanceService attendanceService;
  private final CurrentUserService currentUserService;
  private final LessonSessionService lessonSessionService;
  private final EnrollmentService enrollmentService;

  public AttendanceController(
      AttendanceService attendanceService,
      CurrentUserService currentUserService,
      LessonSessionService lessonSessionService,
      EnrollmentService enrollmentService
  ) {
    this.attendanceService = attendanceService;
    this.currentUserService = currentUserService;
    this.lessonSessionService = lessonSessionService;
    this.enrollmentService = enrollmentService;
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  @ResponseStatus(HttpStatus.CREATED)
  public AttendanceResponse create(@Valid @RequestBody AttendanceCreateRequest request) {
    return attendanceService.create(request, currentUserService.getCurrentTeacherId().orElse(null));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'GROUP')")
  public AttendanceResponse get(@PathVariable Long id) {
    AttendanceResponse response = attendanceService.get(id, currentUserService.getCurrentTeacherId().orElse(null));
    if (currentUserService.isGroupAccount()) {
      ensureGroupEnrollmentAccess(response.enrollmentId());
    }
    return response;
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public AttendanceResponse update(@PathVariable Long id, @Valid @RequestBody AttendanceUpdateRequest request) {
    return attendanceService.update(id, request, currentUserService.getCurrentTeacherId().orElse(null));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'GROUP')")
  public List<AttendanceResponse> findTop50(
      @RequestParam(required = false) Long lessonSessionId,
      @RequestParam(required = false) Long enrollmentId
  ) {
    if (lessonSessionId != null && enrollmentId != null) {
      throw new com.bntu.chinesecourses.exception.BadRequestException("Specify only one filter: lessonSessionId or enrollmentId");
    }
    var teacherId = currentUserService.getCurrentTeacherId().orElse(null);
    if (lessonSessionId != null) {
      if (currentUserService.isGroupAccount()) {
        ensureGroupSessionAccess(lessonSessionId);
      }
      return attendanceService.findTop50ByLessonSession(lessonSessionId, teacherId);
    }
    if (enrollmentId != null) {
      if (currentUserService.isGroupAccount()) {
        ensureGroupEnrollmentAccess(enrollmentId);
      }
      return attendanceService.findTop50ByEnrollment(enrollmentId, teacherId);
    }
    throw new com.bntu.chinesecourses.exception.BadRequestException("Specify filter: lessonSessionId or enrollmentId");
  }

  private void ensureGroupSessionAccess(Long lessonSessionId) {
    Long currentGroupId = currentUserService.getCurrentGroupId()
        .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Group id is not linked"));
    Long sessionGroupId = lessonSessionService.get(lessonSessionId, null).groupId();
    if (!currentGroupId.equals(sessionGroupId)) {
      throw new org.springframework.security.access.AccessDeniedException("Access only to own group attendance");
    }
  }

  private void ensureGroupEnrollmentAccess(Long enrollmentId) {
    Long currentGroupId = currentUserService.getCurrentGroupId()
        .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Group id is not linked"));
    Long enrollmentGroupId = enrollmentService.get(enrollmentId, null).groupId();
    if (enrollmentGroupId == null || !currentGroupId.equals(enrollmentGroupId)) {
      throw new org.springframework.security.access.AccessDeniedException("Access only to own group attendance");
    }
  }

  @PatchMapping("/{id}/archive")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public AttendanceResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return attendanceService.setArchived(id, request.archived(), currentUserService.getCurrentTeacherId().orElse(null));
  }

  @GetMapping("/journal")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'GROUP')")
  public AttendanceJournalResponse journal(
      @RequestParam Long groupId,
      @RequestParam LocalDate from,
      @RequestParam LocalDate to
  ) {
    if (currentUserService.isGroupAccount()) {
      Long currentGroupId = currentUserService.getCurrentGroupId()
          .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Group id is not linked"));
      if (!currentGroupId.equals(groupId)) {
        throw new org.springframework.security.access.AccessDeniedException("Access only to own group attendance");
      }
    }
    return attendanceService.getJournal(groupId, from, to, currentUserService.getCurrentTeacherId().orElse(null));
  }
}
