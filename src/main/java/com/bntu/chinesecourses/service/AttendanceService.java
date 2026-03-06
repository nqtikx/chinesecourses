package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.dto.AttendanceResponse;
import com.bntu.chinesecourses.model.dto.AttendanceUpsertRequest;
import java.util.List;

public interface AttendanceService {

  List<AttendanceResponse> listLessonAttendance(Long lessonSessionId);
  AttendanceResponse upsertAttendance(Long lessonSessionId, AttendanceUpsertRequest request);
}
