package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.ArchiveRequest;
import com.bntu.chinesecourses.model.dto.AttendanceCreateRequest;
import com.bntu.chinesecourses.model.dto.AttendanceResponse;
import com.bntu.chinesecourses.model.dto.AttendanceUpdateRequest;
import com.bntu.chinesecourses.service.AttendanceService;
import com.bntu.chinesecourses.service.CurrentUserService;
import jakarta.validation.Valid;
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

  public AttendanceController(AttendanceService attendanceService, CurrentUserService currentUserService) {
    this.attendanceService = attendanceService;
    this.currentUserService = currentUserService;
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  @ResponseStatus(HttpStatus.CREATED)
  public AttendanceResponse create(@Valid @RequestBody AttendanceCreateRequest request) {
    return attendanceService.create(request, currentUserService.getCurrentTeacherId().orElse(null));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public AttendanceResponse get(@PathVariable Long id) {
    return attendanceService.get(id, currentUserService.getCurrentTeacherId().orElse(null));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public AttendanceResponse update(@PathVariable Long id, @Valid @RequestBody AttendanceUpdateRequest request) {
    return attendanceService.update(id, request, currentUserService.getCurrentTeacherId().orElse(null));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public List<AttendanceResponse> findTop50(
      @RequestParam(required = false) Long lessonSessionId,
      @RequestParam(required = false) Long enrollmentId
  ) {
    if (lessonSessionId != null && enrollmentId != null) {
      throw new com.bntu.chinesecourses.exception.BadRequestException("Specify only one filter: lessonSessionId or enrollmentId");
    }
    var teacherId = currentUserService.getCurrentTeacherId().orElse(null);
    if (lessonSessionId != null) {
      return attendanceService.findTop50ByLessonSession(lessonSessionId, teacherId);
    }
    if (enrollmentId != null) {
      return attendanceService.findTop50ByEnrollment(enrollmentId, teacherId);
    }
    throw new com.bntu.chinesecourses.exception.BadRequestException("Specify filter: lessonSessionId or enrollmentId");
  }

  @PatchMapping("/{id}/archive")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public AttendanceResponse setArchived(@PathVariable Long id, @RequestBody ArchiveRequest request) {
    return attendanceService.setArchived(id, request.archived(), currentUserService.getCurrentTeacherId().orElse(null));
  }
}
