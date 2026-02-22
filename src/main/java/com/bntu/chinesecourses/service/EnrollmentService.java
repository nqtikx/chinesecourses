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
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import java.time.Instant;
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
    if (!personRepository.existsById(request.studentId())) {
      throw new NotFoundException("Student not found id=" + request.studentId());
    }

    if (request.payerId() != null && !personRepository.existsById(request.payerId())) {
      throw new NotFoundException("Payer not found id=" + request.payerId());
    }

    if (!semesterRepository.existsById(request.semesterId())) {
      throw new NotFoundException("Semester not found id=" + request.semesterId());
    }

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

    EnrollmentEntity entity = new EnrollmentEntity(
        null,
        request.studentId(),
        request.payerId(),
        request.semesterId(),
        request.groupId(),
        request.status(),
        request.level(),
        false,
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
    entity.setPayerId(request.payerId());
    entity.setSemesterId(request.semesterId());
    entity.setGroupId(request.groupId());
    entity.setStatus(request.status());
    entity.setLevel(request.level());
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


}
