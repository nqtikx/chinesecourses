package com.bntu.chinesecourses.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bntu.chinesecourses.model.entity.ChineseLevel;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.PersonGuardianEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContractPlaceholderResolverTest {

  private ContractPlaceholderResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new ContractPlaceholderResolver();
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private PersonEntity fullPerson() {
    return new PersonEntity(
        1L,
        "Иванов",
        "Иван",
        "Иванович",
        LocalDate.of(1990, 5, 15),
        "+375291234567",
        "ivan@example.com",
        "г. Минск, ул. Ленина, д. 1",
        "Паспорт",
        "МП",
        "1234567",
        LocalDate.of(2015, 3, 10),
        "Советский РУВД г. Минска",
        "1234567A123PB4",
        false,
        Instant.now());
  }

  private EnrollmentEntity enrollment(LocalDate start, LocalDate end) {
    return new EnrollmentEntity(
        null, null, null, null, null,
        EnrollmentStatus.ACTIVE, ChineseLevel.HSK1,
        false, start, end, null, Instant.now());
  }

  // ── basic person fields ───────────────────────────────────────────────────

  @Test
  void firstName_isMapped() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", null);
    assertThat(map.get("__firstName__")).isEqualTo("Иван");
  }

  @Test
  void lastName_isMapped() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", null);
    assertThat(map.get("__lastName__")).isEqualTo("Иванов");
  }

  @Test
  void fullName_includesMiddleName() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", null);
    assertThat(map.get("__fullName__")).isEqualTo("Иванов Иван Иванович");
  }

  @Test
  void fullName_withoutMiddleName() {
    PersonEntity person = new PersonEntity(
        2L, "Петров", "Пётр", null,
        LocalDate.of(1985, 1, 1), null, null, null,
        null, null, null, null, null, null, false, Instant.now());

    Map<String, String> map = resolver.resolve(person, List.of(), "CTR-2", "КЯ", null);
    assertThat(map.get("__fullName__")).isEqualTo("Петров Пётр");
  }

  @Test
  void phone_isMapped() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", null);
    assertThat(map.get("__phone__")).isEqualTo("+375291234567");
  }

  @Test
  void email_isMapped() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", null);
    assertThat(map.get("__email__")).isEqualTo("ivan@example.com");
  }

  @Test
  void birthDate_isFormattedAsDDMMYYYY() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", null);
    assertThat(map.get("__birthDate__")).isEqualTo("15.05.1990");
  }

  // ── document fields ───────────────────────────────────────────────────────

  @Test
  void documentNumber_isMapped() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", null);
    assertThat(map.get("__documentNumber__")).isEqualTo("1234567");
  }

  @Test
  void passportNumber_isAliasForDocumentNumber() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", null);
    assertThat(map.get("__passportNumber__")).isEqualTo(map.get("__documentNumber__"));
  }

  @Test
  void documentIssueDate_isFormattedAsDDMMYYYY() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", null);
    assertThat(map.get("__documentIssueDate__")).isEqualTo("10.03.2015");
  }

  // ── null / blank fallback ─────────────────────────────────────────────────

  @Test
  void nullPhone_returnsEmptyValue() {
    PersonEntity person = new PersonEntity(
        3L, "Сидоров", "Сидор", null,
        null, null, null, null,
        null, null, null, null, null, null, false, Instant.now());

    Map<String, String> map = resolver.resolve(person, List.of(), "CTR-3", "КЯ", null);
    assertThat(map.get("__phone__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
    assertThat(map.get("__email__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
    assertThat(map.get("__birthDate__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
    assertThat(map.get("__documentIssueDate__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
  }

  @Test
  void blankString_returnsEmptyValue() {
    PersonEntity person = new PersonEntity(
        4L, "Козлов", "Козёл", null,
        null, "  ", "   ", null,
        null, null, null, null, null, null, false, Instant.now());

    Map<String, String> map = resolver.resolve(person, List.of(), "CTR-4", "КЯ", null);
    assertThat(map.get("__phone__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
    assertThat(map.get("__email__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
  }

  // ── guardian ──────────────────────────────────────────────────────────────

  @Test
  void primaryGuardian_isUsedWhenPresent() {
    PersonGuardianEntity secondary = new PersonGuardianEntity(
        1L, 1L, "Вторичный Родитель", "+375291111111", "мать", false, false, Instant.now());
    PersonGuardianEntity primary = new PersonGuardianEntity(
        2L, 1L, "Главный Родитель", "+375292222222", "отец", true, false, Instant.now());

    Map<String, String> map = resolver.resolve(
        fullPerson(), List.of(secondary, primary), "CTR-5", "КЯ", null);

    assertThat(map.get("__guardianName__")).isEqualTo("Главный Родитель");
    assertThat(map.get("__guardianPhone__")).isEqualTo("+375292222222");
  }

  @Test
  void firstGuardian_isUsedWhenNoPrimary() {
    PersonGuardianEntity g1 = new PersonGuardianEntity(
        1L, 1L, "Первый Родитель", "+375291111111", "мать", false, false, Instant.now());
    PersonGuardianEntity g2 = new PersonGuardianEntity(
        2L, 1L, "Второй Родитель", "+375292222222", "отец", false, false, Instant.now());

    Map<String, String> map = resolver.resolve(
        fullPerson(), List.of(g1, g2), "CTR-6", "КЯ", null);

    assertThat(map.get("__guardianName__")).isEqualTo("Первый Родитель");
  }

  @Test
  void noGuardians_returnsEmptyValue() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-7", "КЯ", null);
    assertThat(map.get("__guardianName__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
    assertThat(map.get("__guardianPhone__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
  }

  // ── contract context ──────────────────────────────────────────────────────

  @Test
  void contractNumber_isMapped() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-20260411-ABCD1234", "КЯ", null);
    assertThat(map.get("__contractNumber__")).isEqualTo("CTR-20260411-ABCD1234");
  }

  @Test
  void courseName_isMapped() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯдДиП (В)", null);
    assertThat(map.get("__courseName__")).isEqualTo("КЯдДиП (В)");
  }

  @Test
  void studyDates_areMappedFromEnrollment() {
    EnrollmentEntity e = enrollment(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 5, 31));
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", e);
    assertThat(map.get("__studyStart__")).isEqualTo("01.02.2026");
    assertThat(map.get("__studyEnd__")).isEqualTo("31.05.2026");
  }

  @Test
  void studyDates_returnEmptyValueWhenEnrollmentIsNull() {
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", null);
    assertThat(map.get("__studyStart__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
    assertThat(map.get("__studyEnd__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
  }

  @Test
  void studyDates_returnEmptyValueWhenEnrollmentDatesAreNull() {
    EnrollmentEntity e = enrollment(null, null);
    Map<String, String> map = resolver.resolve(fullPerson(), List.of(), "CTR-1", "КЯ", e);
    assertThat(map.get("__studyStart__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
    assertThat(map.get("__studyEnd__")).isEqualTo(ContractPlaceholderResolver.EMPTY_VALUE);
  }
}
