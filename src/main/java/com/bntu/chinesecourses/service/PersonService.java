package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.PersonCreateRequest;
import com.bntu.chinesecourses.model.dto.PersonResponse;
import com.bntu.chinesecourses.model.dto.PersonUpdateRequest;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.repository.PersonRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonService {

  private final PersonRepository personRepository;

  public PersonService(PersonRepository personRepository) {
    this.personRepository = personRepository;
  }

  @Transactional
  public PersonResponse create(PersonCreateRequest request) {
    PersonEntity entity = new PersonEntity(
        null,
        request.lastName(),
        request.firstName(),
        request.middleName(),
        request.birthDate(),
        request.phone(),
        request.email(),
        false,
        Instant.now()
    );

    PersonEntity saved = personRepository.save(entity);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public PersonResponse get(Long id) {
    return personRepository.findById(id)
        .map(PersonService::toResponse)
        .orElseThrow();
  }

  @Transactional
  public PersonResponse update(Long id, PersonUpdateRequest request) {
    PersonEntity entity = personRepository.findById(id).orElseThrow();
    entity.setLastName(request.lastName());
    entity.setFirstName(request.firstName());
    entity.setMiddleName(request.middleName());
    entity.setBirthDate(request.birthDate());
    entity.setPhone(request.phone());
    entity.setEmail(request.email());
    entity.setArchived(request.archived());
    return toResponse(entity);
  }

  @Transactional(readOnly = true)
  public List<PersonResponse> searchByLastNamePrefix(String prefix) {
    if (prefix == null || prefix.isBlank()) {
      return personRepository.findTop50ByArchivedFalseOrderByLastNameAscFirstNameAsc()
          .stream()
          .map(PersonService::toResponse)
          .toList();
    }

    List<PersonResponse> result = personRepository
        .findTop50ByArchivedFalseAndLastNameStartingWithIgnoreCaseOrderByLastNameAscFirstNameAsc(prefix.trim())
        .stream()
        .map(PersonService::toResponse)
        .toList();

    if (result.isEmpty()) {
      throw new NotFoundException("Persons not found by lastNamePrefix: " + prefix.trim());
    }

    return result;
  }


  private static PersonResponse toResponse(PersonEntity entity) {
    return new PersonResponse(
        entity.getId(),
        entity.getLastName(),
        entity.getFirstName(),
        entity.getMiddleName(),
        entity.getBirthDate(),
        entity.getPhone(),
        entity.getEmail(),
        entity.isArchived(),
        entity.getCreatedAt()
    );
  }

  @Transactional
  public PersonResponse archive(Long id) {
    PersonEntity entity = personRepository.findById(id).orElseThrow();
    entity.setArchived(true);
    return toResponse(entity);
  }

  @Transactional
  public PersonResponse setArchived(Long id, boolean archived) {
    PersonEntity entity = personRepository.findById(id).orElseThrow();
    entity.setArchived(archived);
    return toResponse(entity);
  }

}
