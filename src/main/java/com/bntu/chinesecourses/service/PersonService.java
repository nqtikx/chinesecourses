package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.ConflictException;
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
    String phone = normalizePhone(request.phone());
    String email = normalizeEmail(request.email());

    if (phone != null && personRepository.existsByArchivedFalseAndPhone(phone)) {
      throw new ConflictException("Phone already exists");
    }

    if (email != null && personRepository.existsByArchivedFalseAndEmailIgnoreCase(email)) {
      throw new ConflictException("Email already exists");
    }

    PersonEntity entity = new PersonEntity(
        null,
        request.lastName(),
        request.firstName(),
        request.middleName(),
        request.birthDate(),
        phone,
        email,
        false,
        Instant.now()
    );

    PersonEntity saved = personRepository.save(entity);
    return toResponse(saved);
  }

  @Transactional
  public PersonResponse update(Long id, PersonUpdateRequest request) {
    String phone = normalizePhone(request.phone());
    String email = normalizeEmail(request.email());

    if (phone != null && personRepository.existsByArchivedFalseAndPhoneAndIdNot(phone, id)) {
      throw new ConflictException("Phone already exists");
    }

    if (email != null && personRepository.existsByArchivedFalseAndEmailIgnoreCaseAndIdNot(email, id)) {
      throw new ConflictException("Email already exists");
    }

    PersonEntity entity = personRepository.findById(id).orElseThrow();
    entity.setLastName(request.lastName());
    entity.setFirstName(request.firstName());
    entity.setMiddleName(request.middleName());
    entity.setBirthDate(request.birthDate());
    entity.setPhone(phone);
    entity.setEmail(email);
    entity.setArchived(request.archived());
    return toResponse(entity);
  }
  @Transactional(readOnly = true)
  public PersonResponse get(Long id) {
    return personRepository.findById(id)
        .map(PersonService::toResponse)
        .orElseThrow();
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
  public PersonResponse setArchived(Long id, boolean archived) {
    PersonEntity entity = personRepository.findById(id).orElseThrow();
    entity.setArchived(archived);
    return toResponse(entity);
  }

  private static String normalizePhone(String phone) {
    if (phone == null) {
      return null;
    }
    String trimmed = phone.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.replaceAll("[\\s\\-()]", "");
  }

  private static String normalizeEmail(String email) {
    if (email == null) {
      return null;
    }
    String trimmed = email.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.toLowerCase();
  }

}
