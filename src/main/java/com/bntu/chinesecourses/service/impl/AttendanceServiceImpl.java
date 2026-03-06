package com.bntu.chinesecourses.service.impl;

import com.bntu.chinesecourses.model.dto.AttendanceResponse;
import com.bntu.chinesecourses.model.dto.AttendanceUpsertRequest;
import com.bntu.chinesecourses.model.entity.AttendanceEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.LessonSessionEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.model.entity.TeacherEntity;
import com.bntu.chinesecourses.model.entity.UserRole;
import com.bntu.chinesecourses.repository.AttendanceRepository;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.LessonSessionRepository;
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

  public AttendanceServiceImpl(
      AttendanceRepository attendanceRepository,
      LessonSessionRepository lessonSessionRepository,
      EnrollmentRepository enrollmentRepository
  ) {
    this.attendanceRepository = attendanceRepository;
    this.lessonSessionRepository = lessonSessionRepository;
    this.enrollmentRepository = enrollmentRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AttendanceResponse> listLessonAttendance(Long lessonSessionId) {
    requireTeacherOrAdminCanAccessLesson(lessonSessionId, false);
    return attendanceRepository
        .findTop200ByLessonSessionIdAndArchivedFalseOrderByIdAsc(lessonSessionId)
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Override
  @Transactional
  public AttendanceResponse upsertAttendance(Long lessonSessionId, AttendanceUpsertRequest request) {
    requireTeacherOrAdminCanAccessLesson(lessonSessionId, true);

    LessonSessionEntity lesson =
        lessonSessionRepository
            .findByIdAndArchivedFalse(lessonSessionId)
            .orElseThrow(() -> new IllegalArgumentException("Lesson session not found: " + lessonSessionId));

    EnrollmentEntity enrollment =
        enrollmentRepository
            .findByIdAndArchivedFalse(request.enrollmentId())
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + request.enrollmentId()));

    validateEnrollmentBelongsToLessonGroup(enrollment, lesson);

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

  private void requireTeacherOrAdminCanAccessLesson(Long lessonSessionId, boolean write) {
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
      boolean owned = isLessonOwnedByTeacher(lesson, teacherId);
      if (!owned) {
        throw new AccessDeniedException("Teacher cannot modify attendance for this lesson");
      }
      return;
    }

    boolean ok = isLessonOwnedByTeacher(lesson, teacherId) || isTeacherLinkedToGroup(lesson, teacherId);
    if (!ok) {
      throw new AccessDeniedException("Teacher cannot access this lesson");
    }
  }

  private boolean isLessonOwnedByTeacher(LessonSessionEntity lesson, Long teacherId) {
    TeacherEntity lessonTeacher = lesson.getTeacher();
    if (lessonTeacher == null) {
      return false;
    }
    return teacherId.equals(lessonTeacher.getId());
  }

  private boolean isTeacherLinkedToGroup(LessonSessionEntity lesson, Long teacherId) {
    StudyGroupEntity group = lesson.getGroup();
    if (group == null || group.getTeacher() == null) {
      return false;
    }
    return teacherId.equals(group.getTeacher().getId());
  }

  private void validateEnrollmentBelongsToLessonGroup(EnrollmentEntity enrollment, LessonSessionEntity lesson) {
    StudyGroupEntity lessonGroup = lesson.getGroup();
    if (lessonGroup == null) {
      throw new IllegalArgumentException("Lesson session has no group");
    }
    if (enrollment.getGroupId() == null) {
      throw new IllegalArgumentException("Enrollment has no group assigned");
    }
    if (!lessonGroup.getId().equals(enrollment.getGroupId())) {
      throw new IllegalArgumentException("Enrollment does not belong to this lesson group");
    }
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
