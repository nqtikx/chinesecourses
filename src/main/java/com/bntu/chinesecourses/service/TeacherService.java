package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.dto.TeacherCreateRequest;
import com.bntu.chinesecourses.model.dto.TeacherResponse;

public interface TeacherService {

  TeacherResponse create(TeacherCreateRequest request);
  TeacherResponse get(Long id);
  TeacherResponse setArchived(Long id, boolean archived);
}
