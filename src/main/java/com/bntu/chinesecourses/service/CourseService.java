package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.CourseCreateRequest;
import com.bntu.chinesecourses.model.dto.CourseResponse;
import com.bntu.chinesecourses.model.dto.CourseUpdateRequest;
import com.bntu.chinesecourses.model.entity.CourseEntity;
import com.bntu.chinesecourses.repository.CourseRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

  private final CourseRepository courseRepository;

  public CourseService(CourseRepository courseRepository) {
    this.courseRepository = courseRepository;
  }

  @Transactional
  public CourseResponse create(CourseCreateRequest request) {
    String name = normalizeName(request.name());

    if (courseRepository.existsByArchivedFalseAndNameIgnoreCase(name)) {
      throw new ConflictException("Course name already exists");
    }

    CourseEntity entity = new CourseEntity(
        null,
        name,
        normalizeDescription(request.description()),
        false,
        Instant.now()
    );

    return toResponse(courseRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public CourseResponse get(Long id) {
    return courseRepository.findById(id)
        .map(CourseService::toResponse)
        .orElseThrow(() -> new NotFoundException("Course not found"));
  }

  @Transactional(readOnly = true)
  public List<CourseResponse> listTop50() {
    return courseRepository.findTop50ByArchivedFalseOrderByCreatedAtDesc()
        .stream()
        .map(CourseService::toResponse)
        .toList();
  }

  @Transactional
  public CourseResponse update(Long id, CourseUpdateRequest request) {
    CourseEntity entity = courseRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Course not found"));

    String name = normalizeName(request.name());

    if (courseRepository.existsByArchivedFalseAndNameIgnoreCaseAndIdNot(name, id)) {
      throw new ConflictException("Course name already exists");
    }

    entity.setName(name);
    entity.setDescription(normalizeDescription(request.description()));
    entity.setArchived(request.archived());

    return toResponse(entity);
  }

  @Transactional
  public CourseResponse setArchived(Long id, boolean archived) {
    CourseEntity entity = courseRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Course not found"));
    entity.setArchived(archived);
    return toResponse(entity);
  }

  private static CourseResponse toResponse(CourseEntity entity) {
    return new CourseResponse(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        entity.isArchived(),
        entity.getCreatedAt()
    );
  }

  private static String normalizeName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    return name.trim();
  }

  private static String normalizeDescription(String description) {
    if (description == null) {
      return null;
    }
    String trimmed = description.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
