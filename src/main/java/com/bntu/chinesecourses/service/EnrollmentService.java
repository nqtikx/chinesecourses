package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.dto.EnrollmentResponse;
import com.bntu.chinesecourses.model.dto.EnrollmentUpsertRequest;
import java.util.List;

public interface EnrollmentService {

  EnrollmentResponse create(EnrollmentUpsertRequest request);

  EnrollmentResponse update(Long id, EnrollmentUpsertRequest request);

  EnrollmentResponse get(Long id);

  List<EnrollmentResponse> listBySemester(Long semesterId);

  List<EnrollmentResponse> listByGroup(Long groupId);

  void archive(Long id);
}
