package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.ClassProfileResponse;
import com.bntu.chinesecourses.model.dto.ClassProfileGroupItemResponse;
import com.bntu.chinesecourses.model.dto.GroupScheduleRuleResponse;
import com.bntu.chinesecourses.model.dto.GroupNoteResponse;
import com.bntu.chinesecourses.model.dto.PersonResponse;
import com.bntu.chinesecourses.model.dto.StudyMaterialResponse;
import com.bntu.chinesecourses.model.entity.CourseEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.CourseRepository;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.GroupScheduleRuleRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassProfileService {
  private final GroupAccessService groupAccessService;
  private final GroupScheduleRuleRepository groupScheduleRuleRepository;
  private final StudyMaterialService studyMaterialService;
  private final SemesterRepository semesterRepository;
  private final CourseRepository courseRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final PersonService personService;
  private final CurrentUserService currentUserService;
  private final StudyGroupRepository studyGroupRepository;
  private final GroupNoteService groupNoteService;

  public ClassProfileService(
      GroupAccessService groupAccessService,
      GroupScheduleRuleRepository groupScheduleRuleRepository,
      StudyMaterialService studyMaterialService,
      SemesterRepository semesterRepository,
      CourseRepository courseRepository,
      EnrollmentRepository enrollmentRepository,
      PersonService personService,
      CurrentUserService currentUserService,
      StudyGroupRepository studyGroupRepository,
      GroupNoteService groupNoteService
  ) {
    this.groupAccessService = groupAccessService;
    this.groupScheduleRuleRepository = groupScheduleRuleRepository;
    this.studyMaterialService = studyMaterialService;
    this.semesterRepository = semesterRepository;
    this.courseRepository = courseRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.personService = personService;
    this.currentUserService = currentUserService;
    this.studyGroupRepository = studyGroupRepository;
    this.groupNoteService = groupNoteService;
  }

  @Transactional(readOnly = true)
  public ClassProfileResponse getMyClassProfile() {
    Long personId = currentUserService.getCurrentPersonId()
        .orElseThrow(() -> new NotFoundException("Current user has no person profile"));
    EnrollmentEntity enrollment = enrollmentRepository.findFirstByArchivedFalseAndStudentIdAndStatusOrderByCreatedAtDesc(
            personId, EnrollmentStatus.ACTIVE)
        .orElseThrow(() -> new NotFoundException("No active group for current student"));
    if (enrollment.getGroupId() == null) {
      throw new NotFoundException("Current enrollment has no group");
    }
    return getByGroupId(enrollment.getGroupId());
  }

  @Transactional(readOnly = true)
  public ClassProfileResponse getByGroupId(Long groupId) {
    StudyGroupEntity group = groupAccessService.requireVisibleGroup(groupId);
    Long semesterId = Objects.requireNonNull(group.getSemesterId(), "Group semesterId is null");
    SemesterEntity semester = semesterRepository.findById(semesterId)
        .orElseThrow(() -> new NotFoundException("Semester not found id=" + semesterId));
    Long courseId = Objects.requireNonNull(semester.getCourseId(), "Semester courseId is null");
    CourseEntity course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException("Course not found id=" + courseId));

    String teacherName = "-";
    Long teacherId = null;
    if (group.getTeacher() != null && group.getTeacher().getPerson() != null) {
      teacherId = group.getTeacher().getId();
      PersonEntity tp = group.getTeacher().getPerson();
      teacherName = buildName(tp.getLastName(), tp.getFirstName(), tp.getMiddleName());
    }

    List<GroupScheduleRuleResponse> schedule = groupScheduleRuleRepository
        .findTop50ByArchivedFalseAndGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId)
        .stream()
        .map(r -> new GroupScheduleRuleResponse(
            r.getId(), r.getGroup().getId(), r.getDayOfWeek(), r.getStartTime(), r.getEndTime(),
            r.getRoom(), r.isActive(), r.isArchived(), r.getCreatedAt()))
        .toList();

    List<StudyMaterialResponse> materials = studyMaterialService.listByGroup(groupId);
    List<GroupNoteResponse> notes = groupNoteService.listByGroup(groupId);

    List<PersonResponse> students = enrollmentRepository
        .findByArchivedFalseAndGroupIdAndStatusOrderByCreatedAtDesc(groupId, EnrollmentStatus.ACTIVE)
        .stream()
        .map(EnrollmentEntity::getStudentId)
        .distinct()
        .map(personService::get)
        .toList();

    return new ClassProfileResponse(
        group.getId(),
        group.getName(),
        semester.getId(),
        semester.getName(),
        course.getId(),
        course.getName(),
        teacherId,
        teacherName,
        schedule,
        materials,
        notes,
        students
    );
  }

  @Transactional(readOnly = true)
  public List<ClassProfileGroupItemResponse> listAvailableGroups() {
    List<StudyGroupEntity> groups;
    if (currentUserService.isAdmin()) {
      groups = studyGroupRepository.findTop200ByArchivedFalseOrderByNameAsc();
    } else if (currentUserService.isTeacher()) {
      Long teacherId = currentUserService.getCurrentTeacherId().orElseThrow();
      groups = studyGroupRepository.findByArchivedFalseAndTeacher_IdOrderByNameAsc(teacherId);
    } else {
      Long personId = currentUserService.getCurrentPersonId().orElseThrow();
      List<Long> groupIds = enrollmentRepository.findByArchivedFalseAndStudentIdAndStatusOrderByCreatedAtDesc(
              personId, EnrollmentStatus.ACTIVE)
          .stream()
          .map(EnrollmentEntity::getGroupId)
          .filter(Objects::nonNull)
          .distinct()
          .toList();
      groups = groupIds.stream()
          .map(id -> studyGroupRepository.findByIdAndArchivedFalse(id).orElse(null))
          .filter(Objects::nonNull)
          .toList();
    }
    return groups.stream().map(this::toGroupItem).toList();
  }

  private ClassProfileGroupItemResponse toGroupItem(StudyGroupEntity group) {
    Long semesterId = Objects.requireNonNull(group.getSemesterId(), "Group semesterId is null");
    SemesterEntity semester = semesterRepository.findById(semesterId).orElseThrow();
    Long courseId = Objects.requireNonNull(semester.getCourseId(), "Semester courseId is null");
    CourseEntity course = courseRepository.findById(courseId).orElseThrow();
    String teacherName = "-";
    if (group.getTeacher() != null && group.getTeacher().getPerson() != null) {
      PersonEntity p = group.getTeacher().getPerson();
      teacherName = buildName(p.getLastName(), p.getFirstName(), p.getMiddleName());
    }
    long studentsCount = enrollmentRepository
        .findByArchivedFalseAndGroupIdAndStatusOrderByCreatedAtDesc(group.getId(), EnrollmentStatus.ACTIVE)
        .stream()
        .map(EnrollmentEntity::getStudentId)
        .distinct()
        .count();
    return new ClassProfileGroupItemResponse(
        group.getId(),
        group.getName(),
        course.getName(),
        semester.getName(),
        teacherName,
        studentsCount
    );
  }

  private static String buildName(String lastName, String firstName, String middleName) {
    if (middleName == null || middleName.isBlank()) {
      return lastName + " " + firstName;
    }
    return lastName + " " + firstName + " " + middleName;
  }
}
