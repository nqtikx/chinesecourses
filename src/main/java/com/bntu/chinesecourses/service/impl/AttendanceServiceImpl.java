package com.bntu.chinesecourses.service.impl;

import com.bntu.chinesecourses.model.dto.AttendanceResponse;
import com.bntu.chinesecourses.model.dto.AttendanceUpsertRequest;
import com.bntu.chinesecourses.model.entity.AttendanceEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.LessonSessionEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.model.entity.UserRole;
import com.bntu.chinesecourses.repository.AttendanceRepository;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.LessonSessionRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import com.bntu.chinesecourses.security.SecurityPrincipal;
import com.bntu.chinesecourses.security.SecurityUtils;
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
  @Transactional(readOnly = true)
  public List<AttendanceResponse> listLessonAttendance(Long lessonSessionId) {
    requireCanAccessLesson(lessonSessionId, false);
    return attendanceRepository
        .findTop200ByLessonSessionIdAndArchivedFalseOrderByIdAsc(lessonSessionId)
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Override
  @Transactional
  public AttendanceResponse upsertAttendance(Long lessonSessionId, AttendanceUpsertRequest request) {
    requireCanAccessLesson(lessonSessionId, true);

    LessonSessionEntity lesson =
        lessonSessionRepository
            .findByIdAndArchivedFalse(lessonSessionId)
            .orElseThrow(() -> new IllegalArgumentException("Lesson session not found: " + lessonSessionId));

    EnrollmentEntity enrollment =
        enrollmentRepository
            .findByIdAndArchivedFalse(request.enrollmentId())
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + request.enrollmentId()));

    AttendanceEntity entity =
        attendanceRepository
            .findByLessonSessionIdAndEnrollmentId(lessonSessionId, request.enrollmentId())
            .orElseGet(() -> new AttendanceEntity(
                null,
                lesson,
                enrollment,
                request.status(),
                request.comment(),
                Instant.now(),
                false,
                Instant.now()
            ));

    entity.setStatus(request.status());
    entity.setComment(request.comment());
    entity.setMarkedAt(Instant.now());

    AttendanceEntity saved = attendanceRepository.save(entity);
    return toDto(saved);
  }

  private void requireCanAccessLesson(Long lessonSessionId, boolean write) {
    SecurityPrincipal principal = SecurityUtils.requirePrincipal();
    if (principal.getRole() == UserRole.ADMIN) {
      return;
    }
    if (principal.getRole() != UserRole.TEACHER) {
      throw new AccessDeniedException("Forbidden");
    }
    if (principal.getPersonId() == null) {
      throw new AccessDeniedException("Teacher is not linked to person");
    }

    LessonSessionEntity lesson =
        lessonSessionRepository
            .findByIdAndArchivedFalse(lessonSessionId)
            .orElseThrow(() -> new IllegalArgumentException("Lesson session not found: " + lessonSessionId));

    Long teacherId = principal.getPersonId();

    if (write) {
      boolean ok = isLessonOwnedByTeacher(lesson, teacherId);
      if (!ok) {
        throw new AccessDeniedException("Teacher cannot modify this lesson attendance");
      }
      return;
    }

    boolean ok = isLessonOwnedByTeacher(lesson, teacherId) || isTeacherLinkedToGroup(lesson.getGroupId(), teacherId);
    if (!ok) {
      throw new AccessDeniedException("Teacher cannot access this lesson");
    }
  }

  private boolean isLessonOwnedByTeacher(LessonSessionEntity lesson, Long teacherId) {
    Long lessonTeacherId = lesson.getTeacherId();
    return lessonTeacherId != null && lessonTeacherId.equals(teacherId);
  }

  private boolean isTeacherLinkedToGroup(Long groupId, Long teacherId) {
    if (groupId == null) {
      return false;
    }
    StudyGroupEntity group =
        studyGroupRepository
            .findByIdAndArchivedFalse(groupId)
            .orElse(null);
    if (group == null || group.getTeacher() == null) {
      return false;
    }
    return teacherId.equals(group.getTeacher().getId());
  }

  private AttendanceResponse toDto(AttendanceEntity entity) {
    return new AttendanceResponse(
        entity.getId(),
        entity.getLessonSession().getId(),
        entity.getEnrollment().getId(),
        entity.getStatus(),
        entity.getComment(),
        entity.getMarkedAt(),
        entity.getCreatedAt()
    );
  }
}
