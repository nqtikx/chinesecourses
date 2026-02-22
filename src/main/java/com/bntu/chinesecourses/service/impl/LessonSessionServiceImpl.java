package com.bntu.chinesecourses.service.impl;

import com.bntu.chinesecourses.exception.BadRequestException;
import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.exception.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.bntu.chinesecourses.model.dto.LessonSessionCreateRequest;
import com.bntu.chinesecourses.model.dto.LessonSessionResponse;
import com.bntu.chinesecourses.model.dto.LessonSessionUpdateRequest;
import com.bntu.chinesecourses.model.entity.LessonSessionEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.model.entity.TeacherEntity;
import com.bntu.chinesecourses.repository.LessonSessionRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import com.bntu.chinesecourses.repository.TeacherRepository;
import com.bntu.chinesecourses.service.LessonSessionService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonSessionServiceImpl implements LessonSessionService {

  private final LessonSessionRepository lessonSessionRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final TeacherRepository teacherRepository;

  public LessonSessionServiceImpl(
      LessonSessionRepository lessonSessionRepository,
      StudyGroupRepository studyGroupRepository,
      TeacherRepository teacherRepository
  ) {
    this.lessonSessionRepository = lessonSessionRepository;
    this.studyGroupRepository = studyGroupRepository;
    this.teacherRepository = teacherRepository;
  }

  @Override
  @Transactional
  public LessonSessionResponse create(LessonSessionCreateRequest request) {
    validateDates(request.startsAt(), request.endsAt());

    StudyGroupEntity group = studyGroupRepository.findByIdAndArchivedFalse(request.groupId())
        .orElseThrow(() -> new NotFoundException("Study group not found: id=" + request.groupId()));

    TeacherEntity teacher = resolveTeacher(request.teacherId());

    LessonSessionEntity entity = new LessonSessionEntity(
        null,
        group,
        teacher,
        request.startsAt(),
        request.endsAt(),
        normalizeTopic(request.topic()),
        normalizeRoom(request.room()),
        false,
        false,
        Instant.now()
    );

    LessonSessionEntity saved = lessonSessionRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public LessonSessionResponse get(Long id) {
    return get(id, null);
  }

  @Override
  @Transactional(readOnly = true)
  public LessonSessionResponse get(Long id, Long teacherIdFilter) {
    LessonSessionEntity entity = lessonSessionRepository.findByIdAndArchivedFalse(id)
        .orElseThrow(() -> new NotFoundException("Lesson session not found: id=" + id));
    if (teacherIdFilter != null) {
      StudyGroupEntity group = entity.getGroup();
      Long groupTeacherId = group.getTeacher() == null ? null : group.getTeacher().getId();
      if (!teacherIdFilter.equals(groupTeacherId)) {
        throw new AccessDeniedException("Lesson session does not belong to current teacher's groups");
      }
    }
    return toResponse(entity);
  }

  @Override
  @Transactional
  public LessonSessionResponse update(Long id, LessonSessionUpdateRequest request) {
    validateDates(request.startsAt(), request.endsAt());

    LessonSessionEntity entity = lessonSessionRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Lesson session not found: id=" + id));

    StudyGroupEntity group = studyGroupRepository.findByIdAndArchivedFalse(request.groupId())
        .orElseThrow(() -> new NotFoundException("Study group not found: id=" + request.groupId()));

    TeacherEntity teacher = resolveTeacher(request.teacherId());

    entity.setGroup(group);
    entity.setTeacher(teacher);
    entity.setStartsAt(request.startsAt());
    entity.setEndsAt(request.endsAt());
    entity.setTopic(normalizeTopic(request.topic()));
    entity.setRoom(normalizeRoom(request.room()));
    entity.setCanceled(request.canceled());
    entity.setArchived(request.archived());

    return toResponse(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LessonSessionResponse> findTop50ByGroup(Long groupId) {
    return findTop50ByGroup(groupId, null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LessonSessionResponse> findTop50ByGroup(Long groupId, Long teacherIdFilter) {
    if (teacherIdFilter != null) {
      StudyGroupEntity group = studyGroupRepository.findByIdAndArchivedFalse(groupId)
          .orElseThrow(() -> new NotFoundException("Study group not found: id=" + groupId));
      Long groupTeacherId = group.getTeacher() == null ? null : group.getTeacher().getId();
      if (!teacherIdFilter.equals(groupTeacherId)) {
        throw new AccessDeniedException("Group does not belong to current teacher");
      }
    }
    return lessonSessionRepository.findTop50ByArchivedFalseAndGroupIdOrderByStartsAtDesc(groupId).stream()
        .map(LessonSessionServiceImpl::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public LessonSessionResponse setArchived(Long id, boolean archived) {
    LessonSessionEntity entity = lessonSessionRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Lesson session not found: id=" + id));
    entity.setArchived(archived);
    return toResponse(entity);
  }

  private TeacherEntity resolveTeacher(Long teacherId) {
    if (teacherId == null) {
      return null;
    }
    return teacherRepository.findByIdAndArchivedFalse(teacherId)
        .orElseThrow(() -> new NotFoundException("Teacher not found: id=" + teacherId));
  }

  private static void validateDates(Instant startsAt, Instant endsAt) {
    if (!startsAt.isBefore(endsAt)) {
      throw new BadRequestException("startsAt must be before endsAt");
    }
  }

  private static String normalizeTopic(String topic) {
    if (topic == null) {
      return null;
    }
    String trimmed = topic.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String normalizeRoom(String room) {
    if (room == null) {
      return null;
    }
    String trimmed = room.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static LessonSessionResponse toResponse(LessonSessionEntity entity) {
    Long teacherId = entity.getTeacher() == null ? null : entity.getTeacher().getId();
    return new LessonSessionResponse(
        entity.getId(),
        entity.getGroup().getId(),
        teacherId,
        entity.getStartsAt(),
        entity.getEndsAt(),
        entity.getTopic(),
        entity.getRoom(),
        entity.isCanceled(),
        entity.isArchived(),
        entity.getCreatedAt()
    );
  }
}
