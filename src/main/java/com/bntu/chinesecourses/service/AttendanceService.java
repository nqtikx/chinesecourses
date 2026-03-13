package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.dto.AttendanceCreateRequest;
import com.bntu.chinesecourses.model.dto.AttendanceJournalResponse;
import com.bntu.chinesecourses.model.dto.AttendanceResponse;
import com.bntu.chinesecourses.model.dto.AttendanceUpdateRequest;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

  AttendanceResponse create(AttendanceCreateRequest request);
  /** When teacherIdFilter is non-null, lesson session's group must belong to that teacher. */
  AttendanceResponse create(AttendanceCreateRequest request, Long teacherIdFilter);

  AttendanceResponse get(Long id);
  /** When teacherIdFilter is non-null, attendance's lesson session must belong to that teacher. */
  AttendanceResponse get(Long id, Long teacherIdFilter);

  AttendanceResponse update(Long id, AttendanceUpdateRequest request);
  AttendanceResponse update(Long id, AttendanceUpdateRequest request, Long teacherIdFilter);

  List<AttendanceResponse> findTop50ByLessonSession(Long lessonSessionId);
  List<AttendanceResponse> findTop50ByLessonSession(Long lessonSessionId, Long teacherIdFilter);

  List<AttendanceResponse> findTop50ByEnrollment(Long enrollmentId);
  List<AttendanceResponse> findTop50ByEnrollment(Long enrollmentId, Long teacherIdFilter);

  AttendanceJournalResponse getJournal(Long groupId, LocalDate from, LocalDate to, Long teacherIdFilter);

  AttendanceResponse setArchived(Long id, boolean archived);
  AttendanceResponse setArchived(Long id, boolean archived, Long teacherIdFilter);
}
