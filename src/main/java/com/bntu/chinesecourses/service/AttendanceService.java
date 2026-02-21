package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.dto.AttendanceCreateRequest;
import com.bntu.chinesecourses.model.dto.AttendanceResponse;
import com.bntu.chinesecourses.model.dto.AttendanceUpdateRequest;
import java.util.List;

public interface AttendanceService {

  AttendanceResponse create(AttendanceCreateRequest request);

  AttendanceResponse get(Long id);
  AttendanceResponse update(Long id, AttendanceUpdateRequest request);
  List<AttendanceResponse> findTop50ByLessonSession(Long lessonSessionId);
  List<AttendanceResponse> findTop50ByEnrollment(Long enrollmentId);
  AttendanceResponse setArchived(Long id, boolean archived);
}
