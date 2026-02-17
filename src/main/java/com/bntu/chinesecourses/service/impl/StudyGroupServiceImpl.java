package com.bntu.chinesecourses.service.impl;

import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.StudyGroupCreateRequest;
import com.bntu.chinesecourses.model.dto.StudyGroupResponse;
import com.bntu.chinesecourses.model.dto.StudyGroupUpdateRequest;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.model.entity.TeacherEntity;
import com.bntu.chinesecourses.repository.SemesterRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import com.bntu.chinesecourses.repository.TeacherRepository;
import com.bntu.chinesecourses.service.StudyGroupService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyGroupServiceImpl implements StudyGroupService {

  private final StudyGroupRepository studyGroupRepository;
  private final SemesterRepository semesterRepository;
  private final TeacherRepository teacherRepository;

  public StudyGroupServiceImpl(
      StudyGroupRepository studyGroupRepository,
      SemesterRepository semesterRepository,
      TeacherRepository teacherRepository
  ) {
    this.studyGroupRepository = studyGroupRepository;
    this.semesterRepository = semesterRepository;
    this.teacherRepository = teacherRepository;
  }

  @Override
  @Transactional
  public StudyGroupResponse create(StudyGroupCreateRequest request) {
    SemesterEntity semester = semesterRepository.findById(request.semesterId())
        .orElseThrow(() -> new NotFoundException("Semester not found: id=" + request.semesterId()));

    if (semester.isArchived()) {
      throw new ConflictException("Cannot create group for archived semester: id=" + semester.getId());
    }

    String name = normalizeName(request.name());

    if (studyGroupRepository.existsByArchivedFalseAndSemesterIdAndNameIgnoreCase(semester.getId(), name)) {
      throw new ConflictException("Study group already exists for semesterId=" + semester.getId() + " name=" + name);
    }

    TeacherEntity teacher = resolveTeacher(request.teacherId());

    StudyGroupEntity entity = new StudyGroupEntity(
        null,
        semester,
        teacher,
        name,
        normalizeNotes(request.scheduleNotes()),
        false,
        Instant.now()
    );

    StudyGroupEntity saved = studyGroupRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public StudyGroupResponse get(Long id) {
    StudyGroupEntity entity = studyGroupRepository.findByIdAndArchivedFalse(id)
        .orElseThrow(() -> new NotFoundException("Study group not found: id=" + id));
    return toResponse(entity);
  }

  @Override
  @Transactional
  public StudyGroupResponse update(Long id, StudyGroupUpdateRequest request) {
    StudyGroupEntity entity = studyGroupRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Study group not found: id=" + id));

    SemesterEntity semester = semesterRepository.findById(request.semesterId())
        .orElseThrow(() -> new NotFoundException("Semester not found: id=" + request.semesterId()));

    if (semester.isArchived()) {
      throw new ConflictException("Cannot assign group to archived semester: id=" + semester.getId());
    }

    String name = normalizeName(request.name());

    if (studyGroupRepository.existsByArchivedFalseAndSemesterIdAndNameIgnoreCaseAndIdNot(semester.getId(), name, id)) {
      throw new ConflictException("Study group already exists for semesterId=" + semester.getId() + " name=" + name);
    }

    TeacherEntity teacher = resolveTeacher(request.teacherId());

    entity.setSemester(semester);
    entity.setTeacher(teacher);
    entity.setName(name);
    entity.setScheduleNotes(normalizeNotes(request.scheduleNotes()));
    entity.setArchived(request.archived());

    return toResponse(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<StudyGroupResponse> findTop50BySemester(Long semesterId) {
    return studyGroupRepository.findTop50ByArchivedFalseAndSemesterIdOrderByNameAsc(semesterId).stream()
        .map(StudyGroupServiceImpl::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public StudyGroupResponse setArchived(Long id, boolean archived) {
    StudyGroupEntity entity = studyGroupRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Study group not found: id=" + id));
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

  private static StudyGroupResponse toResponse(StudyGroupEntity entity) {
    Long teacherId = entity.getTeacher() == null ? null : entity.getTeacher().getId();
    return new StudyGroupResponse(
        entity.getId(),
        entity.getSemester().getId(),
        teacherId,
        entity.getName(),
        entity.getScheduleNotes(),
        entity.isArchived(),
        entity.getCreatedAt()
    );
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

  private static String normalizeNotes(String notes) {
    if (notes == null) {
      return null;
    }
    String trimmed = notes.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
