package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.AdminUserListItemResponse;
import com.bntu.chinesecourses.model.dto.ProfileCourseItemResponse;
import com.bntu.chinesecourses.model.dto.ProfileUpdateRequest;
import com.bntu.chinesecourses.model.dto.UserProfileResponse;
import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import com.bntu.chinesecourses.model.entity.CourseEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.model.entity.TeacherEntity;
import com.bntu.chinesecourses.repository.CourseRepository;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import com.bntu.chinesecourses.repository.TeacherRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

  private final AdminUserService adminUserService;
  private final EnrollmentService enrollmentService;
  private final PersonRepository personRepository;
  private final SemesterRepository semesterRepository;
  private final CourseRepository courseRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final TeacherRepository teacherRepository;

  public UserProfileService(
      AdminUserService adminUserService,
      EnrollmentService enrollmentService,
      PersonRepository personRepository,
      SemesterRepository semesterRepository,
      CourseRepository courseRepository,
      StudyGroupRepository studyGroupRepository,
      TeacherRepository teacherRepository
  ) {
    this.adminUserService = adminUserService;
    this.enrollmentService = enrollmentService;
    this.personRepository = personRepository;
    this.semesterRepository = semesterRepository;
    this.courseRepository = courseRepository;
    this.studyGroupRepository = studyGroupRepository;
    this.teacherRepository = teacherRepository;
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getMyProfile(String username) {
    AdminUserEntity user = adminUserService.findByUsername(username)
        .orElseThrow(() -> new NotFoundException("User not found"));
    return buildProfile(user);
  }

  @Transactional
  public UserProfileResponse updateMyProfile(String username, ProfileUpdateRequest request) {
    AdminUserEntity user = adminUserService.findByUsername(username)
        .orElseThrow(() -> new NotFoundException("User not found"));
    Long personId = resolvePersonId(user);
    if (personId == null) {
      throw new NotFoundException("Current user has no linked person profile");
    }
    PersonEntity person = personRepository.findById(personId)
        .orElseThrow(() -> new NotFoundException("Person not found id=" + personId));
    if (request.firstName() != null && !request.firstName().isBlank()) {
      person.setFirstName(request.firstName().trim());
    }
    if (request.lastName() != null && !request.lastName().isBlank()) {
      person.setLastName(request.lastName().trim());
    }
    if (request.middleName() != null) {
      person.setMiddleName(request.middleName().trim().isEmpty() ? null : request.middleName().trim());
    }
    if (request.birthDate() != null) {
      person.setBirthDate(request.birthDate());
    }
    if (request.email() != null) {
      String email = request.email().trim().isEmpty() ? null : request.email().trim().toLowerCase();
      person.setEmail(email);
    }
    if (request.phone() != null) {
      String phone = request.phone().trim().isEmpty() ? null : request.phone().trim();
      person.setPhone(phone);
    }
    return buildProfile(user);
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getByUserId(Long userId) {
    AdminUserEntity user = adminUserService.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found"));
    return buildProfile(user);
  }

  @Transactional(readOnly = true)
  public List<AdminUserListItemResponse> listUsers() {
    return adminUserService.findAll().stream()
        .map(u -> {
          Long personId = resolvePersonId(u);
          PersonEntity person = personId == null ? null : personRepository.findById(personId).orElse(null);
          String fullName = person == null
              ? null
              : buildFullName(person.getLastName(), person.getFirstName(), person.getMiddleName());
          return new AdminUserListItemResponse(u.getId(), u.getUsername(), u.getRole().name(), personId, fullName);
        })
        .toList();
  }

  @Transactional(readOnly = true)
  public Long resolvePersonId(AdminUserEntity user) {
    if (user.getPersonId() != null) {
      return user.getPersonId();
    }
    if (user.getTeacherId() != null) {
      TeacherEntity teacher = teacherRepository.findById(user.getTeacherId()).orElse(null);
      if (teacher != null && teacher.getPerson() != null) {
        return teacher.getPerson().getId();
      }
    }
    return null;
  }

  @Transactional(readOnly = true)
  public UserProfileResponse buildProfile(AdminUserEntity user) {
    Long personId = resolvePersonId(user);
    PersonEntity person = personId == null ? null : personRepository.findById(personId).orElse(null);

    ProfileCourseItemResponse current = null;
    List<ProfileCourseItemResponse> completed = List.of();
    if (personId != null) {
      EnrollmentEntity currentEnrollment = enrollmentService.findCurrentForStudent(personId);
      if (currentEnrollment != null) {
        current = mapProfileItem(currentEnrollment);
      }
      completed = enrollmentService.findCompletedForStudent(personId).stream()
          .map(this::mapProfileItem)
          .toList();
    }

    String fullName = person == null ? null : buildFullName(person.getLastName(), person.getFirstName(), person.getMiddleName());
    String email = person == null ? null : person.getEmail();
    String phone = person == null ? null : person.getPhone();

    return new UserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.getRole().name(),
        personId,
        fullName,
        email,
        phone,
        current,
        completed
    );
  }

  private ProfileCourseItemResponse mapProfileItem(EnrollmentEntity enrollment) {
    SemesterEntity semester = semesterRepository.findById(enrollment.getSemesterId()).orElse(null);
    CourseEntity course = semester == null ? null : courseRepository.findById(semester.getCourseId()).orElse(null);
    StudyGroupEntity group = enrollment.getGroupId() == null ? null : studyGroupRepository.findById(enrollment.getGroupId()).orElse(null);

    String teacherName = null;
    if (group != null && group.getTeacher() != null && group.getTeacher().getPerson() != null) {
      PersonEntity tp = group.getTeacher().getPerson();
      teacherName = buildFullName(tp.getLastName(), tp.getFirstName(), tp.getMiddleName());
    }

    return new ProfileCourseItemResponse(
        enrollment.getId(),
        course == null ? null : course.getId(),
        course == null ? null : course.getName(),
        group == null ? null : group.getId(),
        group == null ? null : group.getName(),
        teacherName,
        enrollment.getStartDate(),
        enrollment.getEndDate(),
        enrollment.getStatus().name()
    );
  }

  private static String buildFullName(String lastName, String firstName, String middleName) {
    if (middleName == null || middleName.isBlank()) {
      return lastName + " " + firstName;
    }
    return lastName + " " + firstName + " " + middleName;
  }
}
