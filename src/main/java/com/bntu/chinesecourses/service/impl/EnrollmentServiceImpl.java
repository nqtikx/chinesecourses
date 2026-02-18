package com.bntu.chinesecourses.service.impl;

import com.bntu.chinesecourses.model.dto.EnrollmentResponse;
import com.bntu.chinesecourses.model.dto.EnrollmentUpsertRequest;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import com.bntu.chinesecourses.service.EnrollmentService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

  private final EnrollmentRepository enrollmentRepository;
  private final PersonRepository personRepository;
  private final SemesterRepository semesterRepository;
  private final StudyGroupRepository studyGroupRepository;

  public EnrollmentServiceImpl(
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

  @Override
  @Transactional
  public EnrollmentResponse create(EnrollmentUpsertRequest request) {
    requirePersonExists(request.studentId(), "Student not found: " + request.studentId());
    if (request.payerId() != null) {
      requirePersonExists(request.payerId(), "Payer not found: " + request.payerId());
    }
    requireSemesterExists(request.semesterId(), "Semester not found: " + request.semesterId());
    if (request.groupId() != null) {
      requireGroupExists(request.groupId(), "Study group not found: " + request.groupId());
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
    return toDto(saved);
  }

  @Override
  @Transactional
  public EnrollmentResponse update(Long id, EnrollmentUpsertRequest request) {
    EnrollmentEntity entity =
        enrollmentRepository
            .findByIdAndArchivedFalse(id)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + id));

    requirePersonExists(request.studentId(), "Student not found: " + request.studentId());
    if (request.payerId() != null) {
      requirePersonExists(request.payerId(), "Payer not found: " + request.payerId());
    }
    requireSemesterExists(request.semesterId(), "Semester not found: " + request.semesterId());
    if (request.groupId() != null) {
      requireGroupExists(request.groupId(), "Study group not found: " + request.groupId());
    }

    entity.setPayerId(request.payerId());
    entity.setSemesterId(request.semesterId());
    entity.setGroupId(request.groupId());
    entity.setStatus(request.status());
    entity.setLevel(request.level());

    EnrollmentEntity saved = enrollmentRepository.save(entity);
    return toDto(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public EnrollmentResponse get(Long id) {
    EnrollmentEntity entity =
        enrollmentRepository
            .findByIdAndArchivedFalse(id)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + id));
    return toDto(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<EnrollmentResponse> listBySemester(Long semesterId) {
    return enrollmentRepository
        .findTop50ByArchivedFalseAndSemesterIdOrderByIdAsc(semesterId)
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<EnrollmentResponse> listByGroup(Long groupId) {
    return enrollmentRepository
        .findTop200ByArchivedFalseAndGroupIdOrderByIdAsc(groupId)
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Override
  @Transactional
  public void archive(Long id) {
    EnrollmentEntity entity =
        enrollmentRepository
            .findByIdAndArchivedFalse(id)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + id));
    entity.setArchived(true);
    enrollmentRepository.save(entity);
  }

  private void requirePersonExists(Long personId, String message) {
    boolean exists = personRepository.existsByArchivedFalseAndId(personId);
    if (!exists) {
      throw new IllegalArgumentException(message);
    }
  }

  private void requireSemesterExists(Long semesterId, String message) {
    boolean exists = semesterRepository.existsByArchivedFalseAndId(semesterId);
    if (!exists) {
      throw new IllegalArgumentException(message);
    }
  }

  private void requireGroupExists(Long groupId, String message) {
    boolean exists =
        studyGroupRepository
            .findByIdAndArchivedFalse(groupId)
            .isPresent();
    if (!exists) {
      throw new IllegalArgumentException(message);
    }
  }

  private EnrollmentResponse toDto(EnrollmentEntity entity) {
    return new EnrollmentResponse(
        entity.getId(),
        entity.getStudentId(),
        entity.getPayerId(),
        entity.getSemesterId(),
        entity.getGroupId(),
        entity.getStatus(),
        entity.getLevel(),
        entity.getCreatedAt()
    );
  }
}
