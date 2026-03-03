package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.exception.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.bntu.chinesecourses.model.dto.EnrollmentCreateRequest;
import com.bntu.chinesecourses.model.dto.EnrollmentResponse;
import com.bntu.chinesecourses.model.dto.EnrollmentUpdateRequest;
import com.bntu.chinesecourses.model.entity.ChineseLevel;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

  private final EnrollmentRepository enrollmentRepository;
  private final PersonRepository personRepository;
  private final SemesterRepository semesterRepository;
  private final StudyGroupRepository studyGroupRepository;

  public EnrollmentService(
      EnrollmentRepository enrollmentRepository,
      PersonRepository personRepository,
      SemesterRepository semesterRepository,
      StudyGroupRepository studyGroupRepository
  ) {
    this.enrollmentRepository = enrollmentRepository;
    this.personRepository = personRepository;
    this.semesterRepository = semesterRepository;
    this.studyGroupRepository = studyGroupRepository;
  }

  @Transactional
  public EnrollmentResponse create(EnrollmentCreateRequest request) {
    if (request.status() == null || request.level() == null) {
      throw new ConflictException("Status and level are required");
    }

    if (!personRepository.existsById(request.studentId())) {
      throw new NotFoundException("Student not found id=" + request.studentId());
    }

    if (request.payerId() != null && !personRepository.existsById(request.payerId())) {
      throw new NotFoundException("Payer not found id=" + request.payerId());
    }

    SemesterEntity semester = semesterRepository.findById(request.semesterId())
        .orElseThrow(() -> new NotFoundException("Semester not found id=" + request.semesterId()));

    if (request.groupId() != null) {
      StudyGroupEntity group = studyGroupRepository.findById(request.groupId())
          .orElseThrow(() -> new NotFoundException("Group not found id=" + request.groupId()));

      if (!group.getSemesterId().equals(request.semesterId())) {
        throw new ConflictException("Group belongs to another semester");
      }
    }

    if (enrollmentRepository.existsByArchivedFalseAndStudentIdAndSemesterId(request.studentId(), request.semesterId())) {
      throw new ConflictException(
          "Enrollment already exists for studentId=" + request.studentId() + " and semesterId=" + request.semesterId()
      );
    }

    if (request.status() == EnrollmentStatus.ACTIVE
        && enrollmentRepository.existsByArchivedFalseAndStudentIdAndStatus(request.studentId(), EnrollmentStatus.ACTIVE)) {
      throw new ConflictException("Student already has IN_PROGRESS enrollment");
    }

    LocalDate startDate = request.startDate() != null ? request.startDate() : semester.getStartDate();

    EnrollmentEntity entity = new EnrollmentEntity(
        null,
        request.studentId(),
        request.payerId(),
        request.semesterId(),
        request.groupId(),
        request.status(),
        request.level(),
        false,
        startDate,
        null,
        null,
        Instant.now()
    );

    EnrollmentEntity saved = enrollmentRepository.save(entity);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public EnrollmentResponse get(Long id) {
    return get(id, null);
  }

  @Transactional(readOnly = true)
  public EnrollmentResponse get(Long id, Long teacherIdFilter) {
    EnrollmentEntity entity = enrollmentRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Enrollment not found: id=" + id));
    if (teacherIdFilter != null && entity.getGroupId() != null) {
      StudyGroupEntity group = studyGroupRepository.findByIdAndArchivedFalse(entity.getGroupId())
          .orElseThrow(() -> new NotFoundException("Study group not found: id=" + entity.getGroupId()));
      Long groupTeacherId = group.getTeacher() == null ? null : group.getTeacher().getId();
      if (!teacherIdFilter.equals(groupTeacherId)) {
        throw new AccessDeniedException("Enrollment does not belong to current teacher's groups");
      }
    } else if (teacherIdFilter != null && entity.getGroupId() == null) {
      throw new AccessDeniedException("Enrollment has no group");
    }
    return toResponse(entity);
  }

  @Transactional
  public EnrollmentResponse update(Long id, EnrollmentUpdateRequest request) {
    EnrollmentEntity entity = enrollmentRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Enrollment not found: id=" + id));

    if (request.status() == null
        || request.level() == null
        || request.semesterId() == null) {
      throw new ConflictException("Invalid enrollment update payload");
    }

    if (request.status() == EnrollmentStatus.ACTIVE
        && entity.getStatus() != EnrollmentStatus.ACTIVE
        && enrollmentRepository.existsByArchivedFalseAndStudentIdAndStatus(entity.getStudentId(), EnrollmentStatus.ACTIVE)) {
      throw new ConflictException("Student already has IN_PROGRESS enrollment");
    }

    entity.setPayerId(request.payerId());
    entity.setSemesterId(request.semesterId());
    entity.setGroupId(request.groupId());
    entity.setStatus(request.status());
    entity.setLevel(request.level());
    if (request.startDate() != null) {
      entity.setStartDate(request.startDate());
    }
    if (request.endDate() != null) {
      entity.setEndDate(request.endDate());
    }
    entity.setArchived(request.archived());
    return toResponse(entity);
  }

  @Transactional(readOnly = true)
  public List<EnrollmentResponse> findTop50(Long semesterId, EnrollmentStatus status, ChineseLevel level) {
    return enrollmentRepository
        .findTop50ByArchivedFalseAndSemesterIdAndStatusAndLevelOrderByCreatedAtDesc(semesterId, status, level)
        .stream()
        .map(EnrollmentService::toResponse)
        .toList();
  }

  /** List enrollments by group. When teacherIdFilter is non-null, group must belong to that teacher. */
  @Transactional(readOnly = true)
  public List<EnrollmentResponse> findTop50ByGroupId(Long groupId, Long teacherIdFilter) {
    if (teacherIdFilter != null) {
      StudyGroupEntity group = studyGroupRepository.findByIdAndArchivedFalse(groupId)
          .orElseThrow(() -> new NotFoundException("Study group not found: id=" + groupId));
      Long groupTeacherId = group.getTeacher() == null ? null : group.getTeacher().getId();
      if (!teacherIdFilter.equals(groupTeacherId)) {
        throw new org.springframework.security.access.AccessDeniedException(
            "Group does not belong to current teacher");
      }
    }
    return enrollmentRepository.findTop50ByArchivedFalseAndGroupIdOrderByCreatedAtDesc(groupId).stream()
        .map(EnrollmentService::toResponse)
        .toList();
  }

  private static EnrollmentResponse toResponse(EnrollmentEntity entity) {
    return new EnrollmentResponse(
        entity.getId(),
        entity.getStudentId(),
        entity.getPayerId(),
        entity.getSemesterId(),
        entity.getGroupId(),
        entity.getStatus(),
        entity.getLevel(),
        entity.isArchived(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getContractNumber(),
        entity.getCreatedAt()
    );
  }

  @Transactional
  public EnrollmentResponse setArchived(Long id, boolean archived) {
    EnrollmentEntity entity = enrollmentRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Enrollment not found: id=" + id));
    entity.setArchived(archived);
    return toResponse(entity);
  }

  @Transactional(readOnly = true)
  public List<EnrollmentEntity> findCompletedForStudent(Long studentId) {
    return enrollmentRepository.findByArchivedFalseAndStudentIdAndStatusOrderByCreatedAtDesc(studentId, EnrollmentStatus.COMPLETED);
  }

  @Transactional(readOnly = true)
  public EnrollmentEntity findCurrentForStudent(Long studentId) {
    return enrollmentRepository.findFirstByArchivedFalseAndStudentIdAndStatusOrderByCreatedAtDesc(
            studentId, EnrollmentStatus.ACTIVE)
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public EnrollmentEntity findForContract(Long studentId, Long courseId, Long groupId) {
    if (groupId != null) {
      EnrollmentEntity enrollment = enrollmentRepository.findFirstByArchivedFalseAndStudentIdAndGroupIdAndStatusOrderByCreatedAtDesc(
              studentId, groupId, EnrollmentStatus.ACTIVE)
          .orElseThrow(() -> new NotFoundException("Active enrollment not found for student/group"));
      SemesterEntity sem = semesterRepository.findById(enrollment.getSemesterId())
          .orElseThrow(() -> new NotFoundException("Semester not found for enrollment"));
      if (!sem.getCourseId().equals(courseId)) {
        throw new ConflictException("Selected group does not belong to selected course");
      }
      return enrollment;
    }
    List<EnrollmentEntity> enrollments = enrollmentRepository.findByArchivedFalseAndStudentIdAndStatusOrderByCreatedAtDesc(
        studentId, EnrollmentStatus.ACTIVE);
    return enrollments.stream()
        .filter(e -> {
          SemesterEntity sem = semesterRepository.findById(e.getSemesterId()).orElse(null);
          return sem != null && sem.getCourseId().equals(courseId);
        })
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Active enrollment not found for student/course"));
  }

  @Transactional
  public EnrollmentEntity completeEnrollment(Long id, LocalDate endDate) {
    EnrollmentEntity entity = enrollmentRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Enrollment not found: id=" + id));
    entity.setStatus(EnrollmentStatus.COMPLETED);
    entity.setEndDate(endDate != null ? endDate : LocalDate.now());
    if (entity.getStartDate() == null) {
      entity.setStartDate(entity.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate());
    }
    return entity;
  }


}
