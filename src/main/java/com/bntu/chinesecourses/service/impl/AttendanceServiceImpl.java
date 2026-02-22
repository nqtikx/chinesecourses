package com.bntu.chinesecourses.service.impl;

import com.bntu.chinesecourses.exception.BadRequestException;
import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.AttendanceCreateRequest;
import com.bntu.chinesecourses.model.dto.AttendanceResponse;
import com.bntu.chinesecourses.model.dto.AttendanceUpdateRequest;
import com.bntu.chinesecourses.model.entity.AttendanceEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.LessonSessionEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.AttendanceRepository;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.LessonSessionRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import com.bntu.chinesecourses.service.AttendanceService;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceServiceImpl implements AttendanceService {

  private final AttendanceRepository attendanceRepository;
  private final LessonSessionRepository lessonSessionRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final StudyGroupRepository studyGroupRepository;

  public AttendanceServiceImpl(
      AttendanceRepository attendanceRepository,
      LessonSessionRepository lessonSessionRepository,
      EnrollmentRepository enrollmentRepository,
      StudyGroupRepository studyGroupRepository
  ) {
    this.attendanceRepository = attendanceRepository;
    this.lessonSessionRepository = lessonSessionRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.studyGroupRepository = studyGroupRepository;
  }

  @Override
  @Transactional
  public AttendanceResponse create(AttendanceCreateRequest request) {
    return create(request, null);
  }

  @Override
  @Transactional
  public AttendanceResponse create(AttendanceCreateRequest request, Long teacherIdFilter) {
    LessonSessionEntity lesson = lessonSessionRepository.findByIdAndArchivedFalse(request.lessonSessionId())
        .orElseThrow(() -> new NotFoundException("Lesson session not found: id=" + request.lessonSessionId()));

    if (teacherIdFilter != null) {
      ensureSessionBelongsToTeacher(lesson, teacherIdFilter);
    }

    if (lesson.isCanceled()) {
      throw new ConflictException("Cannot mark attendance for canceled lesson: id=" + lesson.getId());
    }

    EnrollmentEntity enrollment = enrollmentRepository.findById(request.enrollmentId())
        .orElseThrow(() -> new NotFoundException("Enrollment not found: id=" + request.enrollmentId()));

    if (enrollment.isArchived()) {
      throw new ConflictException("Cannot mark attendance for archived enrollment: id=" + enrollment.getId());
    }

    Long enrollmentGroupId = enrollment.getGroupId();
    Long lessonGroupId = lesson.getGroup().getId();

    if (enrollmentGroupId == null) {
      throw new BadRequestException("Enrollment has no group assigned: enrollmentId=" + enrollment.getId());
    }

    if (!enrollmentGroupId.equals(lessonGroupId)) {
      throw new BadRequestException("Enrollment group does not match lesson group");
    }

    AttendanceEntity entity = new AttendanceEntity(
        null,
        lesson,
        enrollment,
        request.status(),
        normalizeComment(request.comment()),
        Instant.now(),
        false,
        Instant.now()
    );

    AttendanceEntity saved = attendanceRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public AttendanceResponse get(Long id) {
    return get(id, null);
  }

  @Override
  @Transactional(readOnly = true)
  public AttendanceResponse get(Long id, Long teacherIdFilter) {
    AttendanceEntity entity = attendanceRepository.findByIdAndArchivedFalse(id)
        .orElseThrow(() -> new NotFoundException("Attendance not found: id=" + id));
    if (teacherIdFilter != null) {
      ensureSessionBelongsToTeacher(entity.getLessonSession(), teacherIdFilter);
    }
    return toResponse(entity);
  }

  @Override
  @Transactional
  public AttendanceResponse update(Long id, AttendanceUpdateRequest request) {
    return update(id, request, null);
  }

  @Override
  @Transactional
  public AttendanceResponse update(Long id, AttendanceUpdateRequest request, Long teacherIdFilter) {
    AttendanceEntity entity = attendanceRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Attendance not found: id=" + id));
    if (teacherIdFilter != null) {
      ensureSessionBelongsToTeacher(entity.getLessonSession(), teacherIdFilter);
    }
    entity.setStatus(request.status());
    entity.setComment(normalizeComment(request.comment()));
    entity.setMarkedAt(Instant.now());
    entity.setArchived(request.archived());
    return toResponse(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AttendanceResponse> findTop50ByLessonSession(Long lessonSessionId) {
    return findTop50ByLessonSession(lessonSessionId, null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AttendanceResponse> findTop50ByLessonSession(Long lessonSessionId, Long teacherIdFilter) {
    if (teacherIdFilter != null) {
      LessonSessionEntity session = lessonSessionRepository.findByIdAndArchivedFalse(lessonSessionId)
          .orElseThrow(() -> new NotFoundException("Lesson session not found: id=" + lessonSessionId));
      ensureSessionBelongsToTeacher(session, teacherIdFilter);
    }
    return attendanceRepository.findTop50ByArchivedFalseAndLessonSessionIdOrderByMarkedAtDesc(lessonSessionId).stream()
        .map(AttendanceServiceImpl::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AttendanceResponse> findTop50ByEnrollment(Long enrollmentId) {
    return findTop50ByEnrollment(enrollmentId, null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AttendanceResponse> findTop50ByEnrollment(Long enrollmentId, Long teacherIdFilter) {
    if (teacherIdFilter != null) {
      EnrollmentEntity enrollment = enrollmentRepository.findById(enrollmentId)
          .orElseThrow(() -> new NotFoundException("Enrollment not found: id=" + enrollmentId));
      if (enrollment.getGroupId() == null) {
        throw new AccessDeniedException("Enrollment has no group");
      }
      StudyGroupEntity group = studyGroupRepository.findByIdAndArchivedFalse(enrollment.getGroupId())
          .orElseThrow(() -> new NotFoundException("Study group not found: id=" + enrollment.getGroupId()));
      Long groupTeacherId = group.getTeacher() == null ? null : group.getTeacher().getId();
      if (!teacherIdFilter.equals(groupTeacherId)) {
        throw new AccessDeniedException("Enrollment does not belong to current teacher's groups");
      }
    }
    return attendanceRepository.findTop50ByArchivedFalseAndEnrollmentIdOrderByMarkedAtDesc(enrollmentId).stream()
        .map(AttendanceServiceImpl::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public AttendanceResponse setArchived(Long id, boolean archived) {
    return setArchived(id, archived, null);
  }

  @Override
  @Transactional
  public AttendanceResponse setArchived(Long id, boolean archived, Long teacherIdFilter) {
    AttendanceEntity entity = attendanceRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Attendance not found: id=" + id));
    if (teacherIdFilter != null) {
      ensureSessionBelongsToTeacher(entity.getLessonSession(), teacherIdFilter);
    }
    entity.setArchived(archived);
    entity.setMarkedAt(Instant.now());
    return toResponse(entity);
  }

  private static void ensureSessionBelongsToTeacher(LessonSessionEntity session, Long teacherId) {
    Long groupTeacherId = session.getGroup().getTeacher() == null
        ? null
        : session.getGroup().getTeacher().getId();
    if (!teacherId.equals(groupTeacherId)) {
      throw new AccessDeniedException("Lesson session does not belong to current teacher's groups");
    }
  }

  private static String normalizeComment(String comment) {
    if (comment == null) {
      return null;
    }
    String trimmed = comment.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static AttendanceResponse toResponse(AttendanceEntity entity) {
    return new AttendanceResponse(
        entity.getId(),
        entity.getLessonSession().getId(),
        entity.getEnrollment().getId(),
        entity.getStatus(),
        entity.getComment(),
        entity.getMarkedAt(),
        entity.isArchived(),
        entity.getCreatedAt()
    );
  }
}
