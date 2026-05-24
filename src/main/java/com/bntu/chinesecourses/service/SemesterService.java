package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.SemesterCreateRequest;
import com.bntu.chinesecourses.model.dto.SemesterResponse;
import com.bntu.chinesecourses.model.dto.SemesterUpdateRequest;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.repository.CourseRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SemesterService {

  private final SemesterRepository semesterRepository;
  private final CourseRepository courseRepository;

  public SemesterService(SemesterRepository semesterRepository, CourseRepository courseRepository) {
    this.semesterRepository = semesterRepository;
    this.courseRepository = courseRepository;
  }

  @Transactional
  public SemesterResponse create(SemesterCreateRequest request) {
    validateDates(request.startDate(), request.endDate());

    if (!courseRepository.existsById(request.courseId())) {
      throw new NotFoundException("Course not found");
    }

    String name = normalizeName(request.name());

    if (semesterRepository.existsByArchivedFalseAndCourseIdAndNameIgnoreCase(request.courseId(), name)) {
      throw new ConflictException("Semester name already exists for course");
    }

    SemesterEntity entity = new SemesterEntity(
        null,
        request.courseId(),
        name,
        request.startDate(),
        request.endDate(),
        false,
        Instant.now()
    );

    return toResponse(semesterRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public SemesterResponse get(Long id) {
    return semesterRepository.findById(id)
        .map(SemesterService::toResponse)
        .orElseThrow(() -> new NotFoundException("Semester not found"));
  }

  @Transactional(readOnly = true)
  public List<SemesterResponse> listTop50ByCourse(Long courseId) {
    return semesterRepository.findTop50ByArchivedFalseAndCourseIdOrderByStartDateDesc(courseId)
        .stream()
        .map(SemesterService::toResponse)
        .toList();
  }

  @Transactional
  public SemesterResponse update(Long id, SemesterUpdateRequest request) {
    validateDates(request.startDate(), request.endDate());

    SemesterEntity entity = semesterRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Semester not found"));

    if (!courseRepository.existsById(request.courseId())) {
      throw new NotFoundException("Course not found");
    }

    String name = normalizeName(request.name());

    if (semesterRepository.existsByArchivedFalseAndCourseIdAndNameIgnoreCaseAndIdNot(request.courseId(), name, id)) {
      throw new ConflictException("Semester name already exists for course");
    }

    entity.setCourseId(request.courseId());
    entity.setName(name);
    entity.setStartDate(request.startDate());
    entity.setEndDate(request.endDate());
    entity.setArchived(request.archived());

    return toResponse(entity);
  }

  @Transactional
  public SemesterResponse setArchived(Long id, boolean archived) {
    SemesterEntity entity = semesterRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Semester not found"));
    entity.setArchived(archived);
    return toResponse(entity);
  }

  private static SemesterResponse toResponse(SemesterEntity entity) {
    return new SemesterResponse(
        entity.getId(),
        entity.getCourseId(),
        entity.getName(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.isArchived(),
        entity.getCreatedAt()
    );
  }

  private static void validateDates(java.time.LocalDate start, java.time.LocalDate end) {
    if (start == null || end == null) {
      throw new IllegalArgumentException("startDate and endDate must not be null");
    }
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("startDate must be before or equal to endDate");
    }
  }

  private static String normalizeName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    return name.trim();
  }
}
