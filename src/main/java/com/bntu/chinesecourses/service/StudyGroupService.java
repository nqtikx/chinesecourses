package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.dto.StudyGroupCreateRequest;
import com.bntu.chinesecourses.model.dto.StudyGroupResponse;
import com.bntu.chinesecourses.model.dto.StudyGroupUpdateRequest;
import java.util.List;

public interface StudyGroupService {

  StudyGroupResponse create(StudyGroupCreateRequest request);

  StudyGroupResponse get(Long id);
  /** When teacherIdFilter is non-null, returns 403 if group is not assigned to that teacher. */
  StudyGroupResponse get(Long id, Long teacherIdFilter);

  StudyGroupResponse update(Long id, StudyGroupUpdateRequest request);
  List<StudyGroupResponse> findTop50BySemester(Long semesterId);
  /** When teacherIdFilter is non-null, returns only groups assigned to that teacher. */
  List<StudyGroupResponse> findTop50BySemester(Long semesterId, Long teacherIdFilter);
  StudyGroupResponse setArchived(Long id, boolean archived);
}
