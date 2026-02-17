package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.dto.LessonSessionCreateRequest;
import com.bntu.chinesecourses.model.dto.LessonSessionResponse;
import com.bntu.chinesecourses.model.dto.LessonSessionUpdateRequest;
import java.util.List;

public interface LessonSessionService {

  LessonSessionResponse create(LessonSessionCreateRequest request);
  LessonSessionResponse get(Long id);
  LessonSessionResponse update(Long id, LessonSessionUpdateRequest request);
  List<LessonSessionResponse> findTop50ByGroup(Long groupId);
  LessonSessionResponse setArchived(Long id, boolean archived);
}
