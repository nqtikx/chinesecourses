package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.dto.LessonSessionCreateRequest;
import com.bntu.chinesecourses.model.dto.LessonSessionResponse;
import com.bntu.chinesecourses.model.dto.LessonSessionUpdateRequest;
import java.util.List;

public interface LessonSessionService {

  LessonSessionResponse create(LessonSessionCreateRequest request);
  LessonSessionResponse get(Long id);
  /** When teacherIdFilter is non-null, returns 403 if session's group is not assigned to that teacher. */
  LessonSessionResponse get(Long id, Long teacherIdFilter);
  LessonSessionResponse update(Long id, LessonSessionUpdateRequest request);
  List<LessonSessionResponse> findTop50ByGroup(Long groupId);
  /** When teacherIdFilter is non-null, returns 403 if group is not assigned to that teacher. */
  List<LessonSessionResponse> findTop50ByGroup(Long groupId, Long teacherIdFilter);
  LessonSessionResponse setArchived(Long id, boolean archived);
}
