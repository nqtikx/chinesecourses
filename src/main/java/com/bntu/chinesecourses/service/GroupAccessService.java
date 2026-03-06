package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupAccessService {
  private final CurrentUserService currentUserService;
  private final StudyGroupRepository studyGroupRepository;
  private final EnrollmentRepository enrollmentRepository;

  public GroupAccessService(
      CurrentUserService currentUserService,
      StudyGroupRepository studyGroupRepository,
      EnrollmentRepository enrollmentRepository
  ) {
    this.currentUserService = currentUserService;
    this.studyGroupRepository = studyGroupRepository;
    this.enrollmentRepository = enrollmentRepository;
  }

  @Transactional(readOnly = true)
  public StudyGroupEntity requireVisibleGroup(Long groupId) {
    Long id = Objects.requireNonNull(groupId, "groupId is required");
    StudyGroupEntity group = studyGroupRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Group not found id=" + groupId));
    if (currentUserService.isAdmin()) {
      return group;
    }
    if (currentUserService.isTeacher()) {
      Long teacherId = currentUserService.getCurrentTeacherId()
          .orElseThrow(() -> new AccessDeniedException("Teacher id not found"));
      Long groupTeacherId = group.getTeacher() == null ? null : group.getTeacher().getId();
      if (groupTeacherId == null || !groupTeacherId.equals(teacherId)) {
        throw new AccessDeniedException("Group is not assigned to current teacher");
      }
      return group;
    }
    if (currentUserService.isGroupAccount()) {
      Long currentGroupId = currentUserService.getCurrentGroupId()
          .orElseThrow(() -> new AccessDeniedException("Group account has no linked group"));
      if (!id.equals(currentGroupId)) {
        throw new AccessDeniedException("Group account can access only its own group");
      }
      return group;
    }
    Long personId = currentUserService.getCurrentPersonId()
        .orElseThrow(() -> new AccessDeniedException("User has no linked person profile"));
    boolean inGroup = enrollmentRepository.existsByArchivedFalseAndStudentIdAndGroupIdAndStatus(
        personId, id, EnrollmentStatus.ACTIVE);
    if (!inGroup) {
      throw new AccessDeniedException("Group is not assigned to current student");
    }
    return group;
  }

  @Transactional(readOnly = true)
  public StudyGroupEntity requireManageableGroup(Long groupId) {
    StudyGroupEntity group = requireVisibleGroup(groupId);
    if (currentUserService.isAdmin()) {
      return group;
    }
    if (currentUserService.isTeacher()) {
      return group;
    }
    throw new AccessDeniedException("Student cannot modify group resources");
  }
}
