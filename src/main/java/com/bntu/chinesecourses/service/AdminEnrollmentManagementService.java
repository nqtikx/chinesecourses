package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.AdminEnrollmentCreateRequest;
import com.bntu.chinesecourses.model.dto.AdminEnrollmentPatchRequest;
import com.bntu.chinesecourses.model.dto.EnrollmentCreateRequest;
import com.bntu.chinesecourses.model.dto.EnrollmentResponse;
import com.bntu.chinesecourses.model.entity.ChineseLevel;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.CourseRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminEnrollmentManagementService {

  private final AdminUserService adminUserService;
  private final UserProfileService userProfileService;
  private final SemesterRepository semesterRepository;
  private final CourseRepository courseRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final EnrollmentService enrollmentService;

  public AdminEnrollmentManagementService(
      AdminUserService adminUserService,
      UserProfileService userProfileService,
      SemesterRepository semesterRepository,
      CourseRepository courseRepository,
      StudyGroupRepository studyGroupRepository,
      EnrollmentService enrollmentService
  ) {
    this.adminUserService = adminUserService;
    this.userProfileService = userProfileService;
    this.semesterRepository = semesterRepository;
    this.courseRepository = courseRepository;
    this.studyGroupRepository = studyGroupRepository;
    this.enrollmentService = enrollmentService;
  }

  @Transactional
  public EnrollmentResponse assignInProgress(AdminEnrollmentCreateRequest request) {
    if (!courseRepository.existsById(request.courseId())) {
      throw new NotFoundException("Course not found id=" + request.courseId());
    }
    var user = adminUserService.findById(request.userId())
        .orElseThrow(() -> new NotFoundException("User not found id=" + request.userId()));
    Long studentId = userProfileService.resolvePersonId(user);
    if (studentId == null) {
      throw new ConflictException("User is not linked to person profile");
    }

    SemesterEntity semester = semesterRepository.findTop50ByArchivedFalseAndCourseIdOrderByStartDateDesc(request.courseId())
        .stream()
        .max(Comparator.comparing(SemesterEntity::getStartDate))
        .orElseThrow(() -> new NotFoundException("No active semester found for course id=" + request.courseId()));

    if (request.groupId() != null) {
      StudyGroupEntity group = studyGroupRepository.findById(request.groupId())
          .orElseThrow(() -> new NotFoundException("Group not found id=" + request.groupId()));
      if (!group.getSemesterId().equals(semester.getId())) {
        throw new ConflictException("Group does not belong to the selected course active semester");
      }
    }

    return enrollmentService.create(new EnrollmentCreateRequest(
        studentId,
        studentId,
        semester.getId(),
        request.groupId(),
        EnrollmentStatus.ACTIVE,
        ChineseLevel.HSK1,
        request.startDate()
    ));
  }

  @Transactional
  public EnrollmentResponse patchStatus(Long enrollmentId, AdminEnrollmentPatchRequest request) {
    if (!"COMPLETED".equalsIgnoreCase(request.status())) {
      throw new ConflictException("Only COMPLETED status is supported by this endpoint");
    }
    EnrollmentEntity entity = enrollmentService.completeEnrollment(enrollmentId, request.endDate());
    return enrollmentService.get(entity.getId());
  }
}
