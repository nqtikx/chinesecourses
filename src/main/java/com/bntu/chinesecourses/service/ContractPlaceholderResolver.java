package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.PersonGuardianEntity;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Builds a map of template placeholders → values from user profile and contract context.
 *
 * <p>Placeholder format: {@code __camelCaseName__}
 *
 * <p>Extending: add a new {@code map.put("__newField__", ...)} entry to support additional
 * placeholders without changing the processor or template loading logic.
 */
@Component
public class ContractPlaceholderResolver {

  /** Substituted when a profile field is absent or blank. */
  static final String EMPTY_VALUE = "—";

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  /**
   * Resolves all known placeholders for the given person, guardians and contract context.
   *
   * @param person         student/payer profile (must not be null)
   * @param guardians      list of guardians (empty list is OK)
   * @param contractNumber generated contract number
   * @param courseName     name of the course
   * @param enrollment     enrollment record (may be null)
   * @return immutable-like map of placeholder → replacement value
   */
  public Map<String, String> resolve(
      PersonEntity person,
      List<PersonGuardianEntity> guardians,
      String contractNumber,
      String courseName,
      EnrollmentEntity enrollment) {

    Map<String, String> map = new HashMap<>();

    // ── Person: basic fields ────────────────────────────────────────────────
    map.put("__firstName__", safe(person.getFirstName()));
    map.put("__lastName__", safe(person.getLastName()));
    map.put("__middleName__", safe(person.getMiddleName()));
    map.put("__fullName__", buildFullName(person));
    map.put("__birthDate__",
        person.getBirthDate() == null ? EMPTY_VALUE : person.getBirthDate().format(DATE_FORMAT));
    map.put("__phone__", safe(person.getPhone()));
    map.put("__email__", safe(person.getEmail()));
    map.put("__residentialAddress__", safe(person.getResidentialAddress()));

    // ── Person: identity document ───────────────────────────────────────────
    map.put("__documentType__", safe(person.getDocumentType()));
    map.put("__documentSeries__", safe(person.getDocumentSeries()));
    map.put("__documentNumber__", safe(person.getDocumentNumber()));
    map.put("__passportNumber__", safe(person.getDocumentNumber())); // common alias
    map.put("__documentIssueDate__",
        person.getDocumentIssueDate() == null ? EMPTY_VALUE
            : person.getDocumentIssueDate().format(DATE_FORMAT));
    map.put("__documentIssuedBy__", safe(person.getDocumentIssuedBy()));
    map.put("__documentIdentificationNumber__", safe(person.getDocumentIdentificationNumber()));

    // ── Guardian (primary or first available) ──────────────────────────────
    PersonGuardianEntity primary = guardians.stream()
        .filter(PersonGuardianEntity::isPrimaryGuardian)
        .findFirst()
        .orElse(guardians.isEmpty() ? null : guardians.get(0));
    map.put("__guardianName__", primary == null ? EMPTY_VALUE : safe(primary.getFullName()));
    map.put("__guardianPhone__", primary == null ? EMPTY_VALUE : safe(primary.getPhone()));
    map.put("__guardianRelation__", primary == null ? EMPTY_VALUE : safe(primary.getRelationType()));

    // ── Contract context ────────────────────────────────────────────────────
    map.put("__contractNumber__", safe(contractNumber));
    map.put("__courseName__", safe(courseName));

    if (enrollment != null) {
      map.put("__studyStart__",
          enrollment.getStartDate() == null ? EMPTY_VALUE
              : enrollment.getStartDate().format(DATE_FORMAT));
      map.put("__studyEnd__",
          enrollment.getEndDate() == null ? EMPTY_VALUE
              : enrollment.getEndDate().format(DATE_FORMAT));
    } else {
      map.put("__studyStart__", EMPTY_VALUE);
      map.put("__studyEnd__", EMPTY_VALUE);
    }

    return map;
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private String safe(String value) {
    return value == null || value.isBlank() ? EMPTY_VALUE : value.trim();
  }

  private String buildFullName(PersonEntity person) {
    String ln = safe(person.getLastName());
    String fn = safe(person.getFirstName());
    String mn = person.getMiddleName();
    if (mn == null || mn.isBlank()) {
      return (ln + " " + fn).trim();
    }
    return (ln + " " + fn + " " + mn.trim()).trim();
  }
}
