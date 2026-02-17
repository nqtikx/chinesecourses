package com.bntu.chinesecourses.service.impl;

import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.TeacherCreateRequest;
import com.bntu.chinesecourses.model.dto.TeacherResponse;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.TeacherEntity;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.TeacherRepository;
import com.bntu.chinesecourses.service.TeacherService;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherServiceImpl implements TeacherService {

  private final TeacherRepository teacherRepository;
  private final PersonRepository personRepository;

  public TeacherServiceImpl(TeacherRepository teacherRepository, PersonRepository personRepository) {
    this.teacherRepository = teacherRepository;
    this.personRepository = personRepository;
  }

  @Override
  @Transactional
  public TeacherResponse create(TeacherCreateRequest request) {
    Long personId = request.personId();

    PersonEntity person = personRepository.findById(personId)
        .orElseThrow(() -> new NotFoundException("Person not found: id=" + personId));

    if (person.isArchived()) {
      throw new ConflictException("Cannot create teacher for archived person: id=" + personId);
    }

    if (teacherRepository.existsById(personId)) {
      throw new ConflictException("Teacher already exists for personId=" + personId);
    }

    TeacherEntity entity = new TeacherEntity(
        personId,
        person,
        false,
        Instant.now()
    );

    TeacherEntity saved = teacherRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public TeacherResponse get(Long id) {
    TeacherEntity entity = teacherRepository.findByIdAndArchivedFalse(id)
        .orElseThrow(() -> new NotFoundException("Teacher not found: id=" + id));
    return toResponse(entity);
  }

  @Override
  @Transactional
  public TeacherResponse setArchived(Long id, boolean archived) {
    TeacherEntity entity = teacherRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Teacher not found: id=" + id));

    entity.setArchived(archived);
    return toResponse(entity);
  }

  private static TeacherResponse toResponse(TeacherEntity entity) {
    return new TeacherResponse(
        entity.getId(),
        entity.getPerson().getId(),
        entity.isArchived(),
        entity.getCreatedAt()
    );
  }
}
