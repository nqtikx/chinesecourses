package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.dto.StudyGroupCreateRequest;
import com.bntu.chinesecourses.model.dto.StudyGroupResponse;
import com.bntu.chinesecourses.model.dto.StudyGroupUpdateRequest;
import java.util.List;

public interface StudyGroupService {

  StudyGroupResponse create(StudyGroupCreateRequest request);

  StudyGroupResponse get(Long id);
  StudyGroupResponse update(Long id, StudyGroupUpdateRequest request);
  List<StudyGroupResponse> findTop50BySemester(Long semesterId);
  StudyGroupResponse setArchived(Long id, boolean archived);
}
