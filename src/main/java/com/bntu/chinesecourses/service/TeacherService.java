package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.TeacherCreateRequest;
import com.bntu.chinesecourses.model.dto.TeacherResponse;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.TeacherEntity;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.TeacherRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeacherService {

  private final TeacherRepository teacherRepository;
  private final PersonRepository personRepository;

  public TeacherService(TeacherRepository teacherRepository, PersonRepository personRepository) {
    this.teacherRepository = teacherRepository;
    this.personRepository = personRepository;
  }

  @Transactional
  public TeacherResponse create(TeacherCreateRequest request) {
    Long personId = request.personId();

    PersonEntity person = personRepository.findById(personId)
        .orElseThrow(() -> new NotFoundException("Person not found"));

    if (teacherRepository.existsById(personId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Teacher already exists for this person");
    }

    TeacherEntity teacher = new TeacherEntity(person);
    TeacherEntity saved = teacherRepository.save(teacher);

    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<TeacherResponse> listAll() {
    return teacherRepository.findAll().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public TeacherResponse get(Long id) {
    TeacherEntity teacher = teacherRepository.findByIdAndArchivedFalse(id)
        .orElseThrow(() -> new NotFoundException("Teacher not found"));

    return toResponse(teacher);
  }

  @Transactional
  public TeacherResponse setArchived(Long id, boolean archived) {
    TeacherEntity teacher = teacherRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Teacher not found"));

    teacher.setArchived(archived);
    return toResponse(teacher);
  }

  private TeacherResponse toResponse(TeacherEntity teacher) {
    return new TeacherResponse(
        teacher.getId(),
        teacher.getPerson().getId(),
        teacher.isArchived(),
        teacher.getCreatedAt()
    );
  }
}
