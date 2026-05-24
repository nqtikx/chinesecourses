package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.PersonCreateRequest;
import com.bntu.chinesecourses.model.dto.PersonGuardianResponse;
import com.bntu.chinesecourses.model.dto.PersonGuardianUpdateRequest;
import com.bntu.chinesecourses.model.dto.PersonResponse;
import com.bntu.chinesecourses.model.dto.PersonUpdateRequest;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.PersonGuardianEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.PersonGuardianRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonService {

  private final PersonRepository personRepository;
  private final PersonGuardianRepository personGuardianRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final EnrollmentRepository enrollmentRepository;

  public PersonService(
      PersonRepository personRepository,
      PersonGuardianRepository personGuardianRepository,
      StudyGroupRepository studyGroupRepository,
      EnrollmentRepository enrollmentRepository
  ) {
    this.personRepository = personRepository;
    this.personGuardianRepository = personGuardianRepository;
    this.studyGroupRepository = studyGroupRepository;
    this.enrollmentRepository = enrollmentRepository;
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
        normalizeText(request.residentialAddress()),
        normalizeText(request.documentType()),
        normalizeText(request.documentSeries()),
        normalizeText(request.documentNumber()),
        request.documentIssueDate(),
        normalizeText(request.documentIssuedBy()),
        normalizeText(request.documentIdentificationNumber()),
        false,
        Instant.now()
    );

    PersonEntity saved = personRepository.save(entity);
    replaceGuardians(saved.getId(), request.guardians());
    return toResponse(saved, loadGuardians(saved.getId()));
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

    PersonEntity entity = personRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Person not found: id=" + id));
    entity.setLastName(request.lastName());
    entity.setFirstName(request.firstName());
    entity.setMiddleName(request.middleName());
    entity.setBirthDate(request.birthDate());
    entity.setPhone(phone);
    entity.setEmail(email);
    entity.setResidentialAddress(normalizeText(request.residentialAddress()));
    entity.setDocumentType(normalizeText(request.documentType()));
    entity.setDocumentSeries(normalizeText(request.documentSeries()));
    entity.setDocumentNumber(normalizeText(request.documentNumber()));
    entity.setDocumentIssueDate(request.documentIssueDate());
    entity.setDocumentIssuedBy(normalizeText(request.documentIssuedBy()));
    entity.setDocumentIdentificationNumber(normalizeText(request.documentIdentificationNumber()));
    entity.setArchived(request.archived());
    replaceGuardians(entity.getId(), request.guardians());
    return toResponse(entity, loadGuardians(entity.getId()));
  }
  @Transactional(readOnly = true)
  public PersonResponse get(Long id) {
    return get(id, null);
  }

  @Transactional(readOnly = true)
  public PersonResponse get(Long id, Long teacherIdFilter) {
    PersonEntity entity = personRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Person not found: id=" + id));
    if (teacherIdFilter != null) {
      List<Long> teacherGroupIds = studyGroupRepository.findByArchivedFalseAndTeacher_Id(teacherIdFilter).stream()
          .map(StudyGroupEntity::getId)
          .toList();
      if (teacherGroupIds.isEmpty() || !enrollmentRepository.existsByArchivedFalseAndStudentIdAndGroupIdIn(id, teacherGroupIds)) {
        throw new AccessDeniedException("Person is not in current teacher's groups");
      }
    }
    return toResponse(entity, loadGuardians(entity.getId()));
  }

  @Transactional(readOnly = true)
  public List<PersonResponse> searchByLastNamePrefix(String prefix) {
    if (prefix == null || prefix.isBlank()) {
      return personRepository.findTop50ByArchivedFalseOrderByLastNameAscFirstNameAsc()
          .stream()
          .map(p -> toResponse(p, loadGuardians(p.getId())))
          .toList();
    }

    List<PersonResponse> result = personRepository
        .findTop50ByArchivedFalseAndLastNameStartingWithIgnoreCaseOrderByLastNameAscFirstNameAsc(prefix.trim())
        .stream()
        .map(p -> toResponse(p, loadGuardians(p.getId())))
        .toList();

    return result;
  }


  private static PersonResponse toResponse(PersonEntity entity) {
    return toResponse(entity, List.of());
  }

  private static PersonResponse toResponse(PersonEntity entity, List<PersonGuardianResponse> guardians) {
    return new PersonResponse(
        entity.getId(),
        entity.getLastName(),
        entity.getFirstName(),
        entity.getMiddleName(),
        entity.getBirthDate(),
        entity.getPhone(),
        entity.getEmail(),
        entity.getResidentialAddress(),
        entity.getDocumentType(),
        entity.getDocumentSeries(),
        entity.getDocumentNumber(),
        entity.getDocumentIssueDate(),
        entity.getDocumentIssuedBy(),
        entity.getDocumentIdentificationNumber(),
        guardians,
        entity.isArchived(),
        entity.getCreatedAt()
    );
  }
  @Transactional
  public PersonResponse setArchived(Long id, boolean archived) {
    PersonEntity entity = personRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Person not found: id=" + id));
    entity.setArchived(archived);
    return toResponse(entity, loadGuardians(entity.getId()));
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

  private static String normalizeText(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void replaceGuardians(Long childPersonId, List<PersonGuardianUpdateRequest> guardians) {
    if (guardians == null) {
      return;
    }
    List<PersonGuardianEntity> existing = personGuardianRepository.findByChildPersonIdAndArchivedFalseOrderByPrimaryGuardianDescCreatedAtAsc(childPersonId);
    for (PersonGuardianEntity entity : existing) {
      entity.setArchived(true);
    }
    for (PersonGuardianUpdateRequest guardian : guardians) {
      String fullName = normalizeText(guardian.fullName());
      if (fullName == null) {
        continue;
      }
      personGuardianRepository.save(new PersonGuardianEntity(
          null,
          childPersonId,
          fullName,
          normalizePhone(guardian.phone()),
          normalizeText(guardian.relationType()),
          guardian.primaryGuardian(),
          guardian.archived(),
          Instant.now()));
    }
  }

  private List<PersonGuardianResponse> loadGuardians(Long personId) {
    return personGuardianRepository.findByChildPersonIdAndArchivedFalseOrderByPrimaryGuardianDescCreatedAtAsc(personId)
        .stream()
        .map(g -> new PersonGuardianResponse(
            g.getId(),
            g.getChildPersonId(),
            g.getFullName(),
            g.getPhone(),
            g.getRelationType(),
            g.isPrimaryGuardian(),
            g.isArchived(),
            g.getCreatedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public PersonResponse getDetailed(Long id) {
    PersonEntity person = personRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Person not found: id=" + id));
    return toResponse(person, loadGuardians(id));
  }
}
