package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.AdminUserListItemResponse;
import com.bntu.chinesecourses.model.dto.PersonGuardianResponse;
import com.bntu.chinesecourses.model.dto.PersonGuardianUpdateRequest;
import com.bntu.chinesecourses.model.dto.ProfileCourseItemResponse;
import com.bntu.chinesecourses.model.dto.ProfileUpdateRequest;
import com.bntu.chinesecourses.model.dto.UserProfileResponse;
import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import com.bntu.chinesecourses.model.entity.CourseEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.PersonGuardianEntity;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.model.entity.TeacherEntity;
import com.bntu.chinesecourses.repository.CourseRepository;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.PersonGuardianRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import com.bntu.chinesecourses.repository.TeacherRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

  private final AdminUserService adminUserService;
  private final EnrollmentService enrollmentService;
  private final PersonRepository personRepository;
  private final PersonGuardianRepository personGuardianRepository;
  private final SemesterRepository semesterRepository;
  private final CourseRepository courseRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final TeacherRepository teacherRepository;
  private final SemesterDiscountService semesterDiscountService;

  public UserProfileService(
      AdminUserService adminUserService,
      EnrollmentService enrollmentService,
      PersonRepository personRepository,
      PersonGuardianRepository personGuardianRepository,
      SemesterRepository semesterRepository,
      CourseRepository courseRepository,
      StudyGroupRepository studyGroupRepository,
      TeacherRepository teacherRepository,
      SemesterDiscountService semesterDiscountService
  ) {
    this.adminUserService = adminUserService;
    this.enrollmentService = enrollmentService;
    this.personRepository = personRepository;
    this.personGuardianRepository = personGuardianRepository;
    this.semesterRepository = semesterRepository;
    this.courseRepository = courseRepository;
    this.studyGroupRepository = studyGroupRepository;
    this.teacherRepository = teacherRepository;
    this.semesterDiscountService = semesterDiscountService;
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
    if (request.residentialAddress() != null) {
      person.setResidentialAddress(normalizeText(request.residentialAddress()));
    }
    if (request.documentType() != null) {
      person.setDocumentType(normalizeText(request.documentType()));
    }
    if (request.documentSeries() != null) {
      person.setDocumentSeries(normalizeText(request.documentSeries()));
    }
    if (request.documentNumber() != null) {
      person.setDocumentNumber(normalizeText(request.documentNumber()));
    }
    if (request.documentIssueDate() != null) {
      person.setDocumentIssueDate(request.documentIssueDate());
    }
    if (request.documentIssuedBy() != null) {
      person.setDocumentIssuedBy(normalizeText(request.documentIssuedBy()));
    }
    if (request.documentIdentificationNumber() != null) {
      person.setDocumentIdentificationNumber(normalizeText(request.documentIdentificationNumber()));
    }
    if (request.guardians() != null) {
      replaceGuardians(personId, request.guardians());
    }
    return buildProfile(user);
  }

  /** Admin-only: update another user's person profile by their user id. */
  @Transactional
  public UserProfileResponse updateByUserId(Long userId, ProfileUpdateRequest request) {
    AdminUserEntity user = adminUserService.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found id=" + userId));
    Long personId = resolvePersonId(user);
    if (personId == null) {
      throw new NotFoundException("User id=" + userId + " has no linked person profile");
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
    if (request.residentialAddress() != null) {
      person.setResidentialAddress(normalizeText(request.residentialAddress()));
    }
    if (request.documentType() != null) {
      person.setDocumentType(normalizeText(request.documentType()));
    }
    if (request.documentSeries() != null) {
      person.setDocumentSeries(normalizeText(request.documentSeries()));
    }
    if (request.documentNumber() != null) {
      person.setDocumentNumber(normalizeText(request.documentNumber()));
    }
    if (request.documentIssueDate() != null) {
      person.setDocumentIssueDate(request.documentIssueDate());
    }
    if (request.documentIssuedBy() != null) {
      person.setDocumentIssuedBy(normalizeText(request.documentIssuedBy()));
    }
    if (request.documentIdentificationNumber() != null) {
      person.setDocumentIdentificationNumber(normalizeText(request.documentIdentificationNumber()));
    }
    if (request.guardians() != null) {
      replaceGuardians(personId, request.guardians());
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
      TeacherEntity teacher = teacherRepository.findById(Objects.requireNonNull(user.getTeacherId())).orElse(null);
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
    SemesterDiscountService.DiscountEvaluation discountEvaluation =
        new SemesterDiscountService.DiscountEvaluation(0, 0, 0, false);
    if (personId != null) {
      EnrollmentEntity currentEnrollment = enrollmentService.findCurrentForStudent(personId);
      List<EnrollmentEntity> completedEnrollments = enrollmentService.findCompletedForStudent(personId);
      if (currentEnrollment != null) {
        current = mapProfileItem(currentEnrollment);
        discountEvaluation = semesterDiscountService.evaluate(personId, currentEnrollment.getSemesterId());
      }
      completed = completedEnrollments.stream()
          .map(this::mapProfileItem)
          .toList();
      if (currentEnrollment == null && !completedEnrollments.isEmpty()) {
        EnrollmentEntity latestCompleted = completedEnrollments.getFirst();
        discountEvaluation = semesterDiscountService.evaluate(personId, latestCompleted.getSemesterId());
      }
    }

    String fullName = person == null ? null : buildFullName(person.getLastName(), person.getFirstName(), person.getMiddleName());
    String email = person == null ? null : person.getEmail();
    String phone = person == null ? null : person.getPhone();
    String residentialAddress = person == null ? null : person.getResidentialAddress();
    String documentType = person == null ? null : person.getDocumentType();
    String documentSeries = person == null ? null : person.getDocumentSeries();
    String documentNumber = person == null ? null : person.getDocumentNumber();
    java.time.LocalDate documentIssueDate = person == null ? null : person.getDocumentIssueDate();
    String documentIssuedBy = person == null ? null : person.getDocumentIssuedBy();
    String documentIdentificationNumber = person == null ? null : person.getDocumentIdentificationNumber();
    List<PersonGuardianResponse> guardians = personId == null ? List.of() : loadGuardians(personId);

    String teacherFullName = null;
    String teacherPhone = null;
    String teacherEmail = null;
    if (current != null && current.groupId() != null) {
      StudyGroupEntity group = studyGroupRepository.findById(Objects.requireNonNull(current.groupId())).orElse(null);
      if (group != null && group.getTeacher() != null && group.getTeacher().getPerson() != null) {
        PersonEntity tp = group.getTeacher().getPerson();
        teacherFullName = buildFullName(tp.getLastName(), tp.getFirstName(), tp.getMiddleName());
        teacherPhone = tp.getPhone();
        teacherEmail = tp.getEmail();
      }
    }

    return new UserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.getRole().name(),
        personId,
        fullName,
        email,
        phone,
        residentialAddress,
        documentType,
        documentSeries,
        documentNumber,
        documentIssueDate,
        documentIssuedBy,
        documentIdentificationNumber,
        guardians,
        teacherFullName,
        teacherPhone,
        teacherEmail,
        current,
        completed,
        discountEvaluation.completedCoursesCount(),
        discountEvaluation.consecutiveSemesterStreak(),
        discountEvaluation.nextDiscountPercent(),
        discountEvaluation.discountResetByGap()
    );
  }

  private ProfileCourseItemResponse mapProfileItem(EnrollmentEntity enrollment) {
    SemesterEntity semester = semesterRepository.findById(Objects.requireNonNull(enrollment.getSemesterId())).orElse(null);
    CourseEntity course = semester == null ? null : courseRepository.findById(Objects.requireNonNull(semester.getCourseId())).orElse(null);
    StudyGroupEntity group = enrollment.getGroupId() == null ? null : studyGroupRepository.findById(Objects.requireNonNull(enrollment.getGroupId())).orElse(null);

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
        enrollment.getStartDate() != null ? enrollment.getStartDate() : (semester == null ? null : semester.getStartDate()),
        enrollment.getEndDate() != null ? enrollment.getEndDate() : (semester == null ? null : semester.getEndDate()),
        enrollment.getStatus().name()
    );
  }

  private static String buildFullName(String lastName, String firstName, String middleName) {
    if (middleName == null || middleName.isBlank()) {
      return lastName + " " + firstName;
    }
    return lastName + " " + firstName + " " + middleName;
  }

  private static String normalizeText(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private List<PersonGuardianResponse> loadGuardians(Long personId) {
    return personGuardianRepository.findByChildPersonIdAndArchivedFalseOrderByPrimaryGuardianDescCreatedAtAsc(personId)
        .stream()
        .map(g -> new PersonGuardianResponse(
            g.getId(),
            g.getChildPersonId(),
            g.getFullName(),
            g.getPhone(),
            g.getRelationType(),
            g.isPrimaryGuardian(),
            g.isArchived(),
            g.getCreatedAt()))
        .toList();
  }

  private void replaceGuardians(Long childPersonId, List<PersonGuardianUpdateRequest> guardians) {
    List<PersonGuardianEntity> existing = personGuardianRepository.findByChildPersonIdAndArchivedFalseOrderByPrimaryGuardianDescCreatedAtAsc(childPersonId);
    for (PersonGuardianEntity item : existing) {
      item.setArchived(true);
    }
    for (PersonGuardianUpdateRequest guardian : guardians) {
      String fullName = normalizeText(guardian.fullName());
      if (fullName == null) {
        continue;
      }
      personGuardianRepository.save(new PersonGuardianEntity(
          null,
          childPersonId,
          fullName,
          normalizeText(guardian.phone()),
          normalizeText(guardian.relationType()),
          guardian.primaryGuardian(),
          guardian.archived(),
          Instant.now()));
    }
  }
}
