package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.model.dto.EnrollmentCreateRequest;
import com.bntu.chinesecourses.model.dto.EnrollmentResponse;
import com.bntu.chinesecourses.model.dto.EnrollmentUpdateRequest;
import com.bntu.chinesecourses.model.entity.ChineseLevel;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

  private final EnrollmentRepository enrollmentRepository;

  public EnrollmentService(EnrollmentRepository enrollmentRepository) {
    this.enrollmentRepository = enrollmentRepository;
  }

  @Transactional
  public EnrollmentResponse create(EnrollmentCreateRequest request) {
    if (enrollmentRepository.existsByArchivedFalseAndStudentIdAndSemesterId(request.studentId(), request.semesterId())) {
      throw new ConflictException("Enrollment already exists for studentId=" + request.studentId()
          + " and semesterId=" + request.semesterId());
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
    return enrollmentRepository.findById(id)
        .map(EnrollmentService::toResponse)
        .orElseThrow();
  }

  @Transactional
  public EnrollmentResponse update(Long id, EnrollmentUpdateRequest request) {
    EnrollmentEntity entity = enrollmentRepository.findById(id).orElseThrow();
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
  public EnrollmentResponse archive(Long id) {
    EnrollmentEntity entity = enrollmentRepository.findById(id).orElseThrow();
    entity.setArchived(true);
    return toResponse(entity);
  }

}
