package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.AttendanceResponse;
import com.bntu.chinesecourses.model.dto.AttendanceUpsertRequest;
import com.bntu.chinesecourses.service.AttendanceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lesson-sessions/{lessonSessionId}/attendance")
public class AttendanceController {

  private final AttendanceService attendanceService;

  public AttendanceController(AttendanceService attendanceService) {
    this.attendanceService = attendanceService;
  }

  @GetMapping
  public List<AttendanceResponse> list(@PathVariable Long lessonSessionId) {
    return attendanceService.listLessonAttendance(lessonSessionId);
  }

  @PutMapping
  public AttendanceResponse upsert(
      @PathVariable Long lessonSessionId,
      @RequestBody @Valid AttendanceUpsertRequest request
  ) {
    return attendanceService.upsertAttendance(lessonSessionId, request);
  }
}
