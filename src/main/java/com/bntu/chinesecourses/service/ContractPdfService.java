package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.ContractDocumentResponse;
import com.bntu.chinesecourses.model.dto.AdminContractCreateRequest;
import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import com.bntu.chinesecourses.model.entity.ContractDocumentEntity;
import com.bntu.chinesecourses.model.entity.CourseEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.ContractDocumentRepository;
import com.bntu.chinesecourses.repository.CourseRepository;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractPdfService {
  private static final String CONTRACT_FONT = "Times New Roman";

  private final AdminUserService adminUserService;
  private final UserProfileService userProfileService;
  private final EnrollmentService enrollmentService;
  private final EnrollmentRepository enrollmentRepository;
  private final PersonRepository personRepository;
  private final CourseRepository courseRepository;
  private final SemesterRepository semesterRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final ContractDocumentRepository contractDocumentRepository;
  private final FileStorageService fileStorageService;
  private final CurrentUserService currentUserService;
  private final GroupAccessService groupAccessService;
  private final int repeatDiscountPercent;
  private final BigDecimal semesterBasePrice;

  public ContractPdfService(
      AdminUserService adminUserService,
      UserProfileService userProfileService,
      EnrollmentService enrollmentService,
      EnrollmentRepository enrollmentRepository,
      PersonRepository personRepository,
      CourseRepository courseRepository,
      SemesterRepository semesterRepository,
      StudyGroupRepository studyGroupRepository,
      ContractDocumentRepository contractDocumentRepository,
      FileStorageService fileStorageService,
      CurrentUserService currentUserService,
      GroupAccessService groupAccessService,
      @Value("${app.contracts.repeat-discount-percent:10}") int repeatDiscountPercent,
      @Value("${app.contracts.base-price:1200}") BigDecimal semesterBasePrice
  ) {
    this.adminUserService = adminUserService;
    this.userProfileService = userProfileService;
    this.enrollmentService = enrollmentService;
    this.enrollmentRepository = enrollmentRepository;
    this.personRepository = personRepository;
    this.courseRepository = courseRepository;
    this.semesterRepository = semesterRepository;
    this.studyGroupRepository = studyGroupRepository;
    this.contractDocumentRepository = contractDocumentRepository;
    this.fileStorageService = fileStorageService;
    this.currentUserService = currentUserService;
    this.groupAccessService = groupAccessService;
    this.repeatDiscountPercent = repeatDiscountPercent;
    this.semesterBasePrice = semesterBasePrice;
  }

  @Transactional
  public GeneratedContract generate(AdminContractCreateRequest request) {
    Long userId = Objects.requireNonNull(request.userId(), "userId is required");
    Long courseId = Objects.requireNonNull(request.courseId(), "courseId is required");
    AdminUserEntity user = adminUserService.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found id=" + userId));
    Long personId = userProfileService.resolvePersonId(user);
    if (personId == null) {
      throw new NotFoundException("User is not linked to person profile");
    }
    PersonEntity person = personRepository.findById(personId)
        .orElseThrow(() -> new NotFoundException("Person not found id=" + personId));
    CourseEntity course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException("Course not found id=" + courseId));

    EnrollmentEntity enrollment = enrollmentService.findForContract(personId, courseId, request.groupId());
    Long semesterId = Objects.requireNonNull(enrollment.getSemesterId(), "Enrollment semesterId is required");
    SemesterEntity semester = semesterRepository.findById(semesterId)
        .orElseThrow(() -> new NotFoundException("Semester not found id=" + semesterId));
    Long groupId = enrollment.getGroupId();
    StudyGroupEntity group = groupId == null ? null : studyGroupRepository.findById(groupId).orElse(null);

    String contractNumber = enrollment.getContractNumber();
    if (contractNumber == null || contractNumber.isBlank()) {
      contractNumber = generateUniqueContractNumber();
      enrollment.setContractNumber(contractNumber);
    }

    int discountPercent = enrollmentService.findCompletedForStudent(personId).isEmpty() ? 0 : repeatDiscountPercent;
    BigDecimal discount = semesterBasePrice
        .multiply(BigDecimal.valueOf(discountPercent))
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    BigDecimal finalPrice = semesterBasePrice.subtract(discount).setScale(2, RoundingMode.HALF_UP);

    String teacherName = group == null || group.getTeacher() == null || group.getTeacher().getPerson() == null
        ? "-"
        : buildFullName(
            group.getTeacher().getPerson().getLastName(),
            group.getTeacher().getPerson().getFirstName(),
            group.getTeacher().getPerson().getMiddleName());

    byte[] wordBytes = buildWord(
        contractNumber,
        person,
        course.getName(),
        group == null ? "-" : group.getName(),
        teacherName,
        LocalDate.now(),
        semester.getName(),
        semesterBasePrice,
        discountPercent,
        finalPrice
    );
    String filePath = fileStorageService.saveContract(wordBytes, "docx");
    Long generatedBy = currentUserService.getCurrentUserId().orElse(0L);
    String fileName = "contract-" + contractNumber + ".docx";
    contractDocumentRepository.save(new ContractDocumentEntity(
        null,
        enrollment.getId(),
        user.getId(),
        course.getId(),
        group == null ? null : group.getId(),
        contractNumber,
        fileName,
        filePath,
        semesterBasePrice,
        discountPercent,
        finalPrice,
        generatedBy,
        Instant.now()
    ));

    return new GeneratedContract(contractNumber, wordBytes, fileName);
  }

  @Transactional(readOnly = true)
  public List<ContractDocumentResponse> listMyContracts() {
    Long currentUserId = currentUserService.getCurrentUserId()
        .orElseThrow(() -> new NotFoundException("Current user not found"));
    return contractDocumentRepository.findTop200ByUserIdOrderByCreatedAtDesc(currentUserId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ContractDocumentResponse> listAllContracts() {
    return contractDocumentRepository.findTop500ByOrderByCreatedAtDesc()
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ContractDocumentResponse> listByGroup(Long groupId) {
    Objects.requireNonNull(groupId, "groupId is required");
    groupAccessService.requireVisibleGroup(groupId);
    return contractDocumentRepository.findTop200ByGroupIdOrderByCreatedAtDesc(groupId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public StoredContractFile download(Long id) {
    Long contractId = Objects.requireNonNull(id, "contract id is required");
    ContractDocumentEntity doc = contractDocumentRepository.findById(contractId)
        .orElseThrow(() -> new NotFoundException("Contract not found"));
    if (!currentUserService.isAdmin()) {
      Long currentUserId = currentUserService.getCurrentUserId().orElse(null);
      if (currentUserId == null) {
        throw new AccessDeniedException("No current user");
      }
      if (Objects.equals(currentUserId, doc.getUserId())) {
        return new StoredContractFile(doc.getFileName(), fileStorageService.loadAsResource(doc.getStoragePath()));
      }
      if (doc.getGroupId() != null && currentUserService.isTeacher()) {
        groupAccessService.requireVisibleGroup(doc.getGroupId());
        return new StoredContractFile(doc.getFileName(), fileStorageService.loadAsResource(doc.getStoragePath()));
      }
      throw new AccessDeniedException("No access to this contract");
    }
    return new StoredContractFile(doc.getFileName(), fileStorageService.loadAsResource(doc.getStoragePath()));
  }

  private String generateUniqueContractNumber() {
    String prefix = "CTR-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
    for (int i = 0; i < 20; i++) {
      String candidate = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
      if (!enrollmentRepository.existsByContractNumber(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("Unable to generate unique contract number");
  }

  private byte[] buildWord(
      String contractNumber,
      PersonEntity person,
      String courseName,
      String groupName,
      String teacherName,
      LocalDate generatedAt,
      String semesterName,
      BigDecimal basePrice,
      int discountPercent,
      BigDecimal finalPrice
  ) {
    try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      addCenteredBold(document, "ДОГОВОР № " + contractNumber, 14);
      addCenteredBold(document, "о платных услугах в сфере образования", 12);
      addCentered(document, "(на обучение по программе изучения китайского языка)", 11);
      addSpacer(document, 1);

      addLeft(document, "г. Минск                                      «___» __________ " + generatedAt.getYear() + " г.", 11);
      addSpacer(document, 1);

      String fullName = buildFullName(person.getLastName(), person.getFirstName(), person.getMiddleName());
      addJustified(document,
          "Белорусский национальный технический университет (далее – БНТУ), с одной стороны, и "
              + fullName + " (далее – Слушатель), с другой стороны, заключили настоящий договор о нижеследующем:",
          12, false);
      addSpacer(document, 1);

      addCenteredBold(document, "1. Предмет договора", 12);
      addJustified(document,
          "1.1. Освоение Слушателем образовательной программы дополнительного образования «"
              + courseName + "», семестр: " + semesterName + ", группа: " + groupName + ".", 11, false);
      addJustified(document,
          "1.2. Руководитель/преподаватель группы: " + teacherName + ".", 11, false);
      addJustified(document,
          "1.3. Контактные данные Слушателя: дата рождения — "
              + (person.getBirthDate() == null ? "-" : person.getBirthDate()) + ", email — "
              + safe(person.getEmail()) + ", телефон — " + safe(person.getPhone()) + ".", 11, false);

      addSpacer(document, 1);
      addCenteredBold(document, "2. Стоимость обучения и скидка", 12);
      addJustified(document, "2.1. Базовая стоимость обучения: " + basePrice.toPlainString() + " BYN.", 11, false);
      addJustified(document, "2.2. Скидка за повторное обучение: " + discountPercent + "%.", 11, false);
      addJustified(document, "2.3. Итоговая стоимость к оплате: " + finalPrice.toPlainString() + " BYN.", 11, true);

      addSpacer(document, 1);
      addCenteredBold(document, "3. Права и обязанности сторон", 12);
      addJustified(document, "3.1. БНТУ обязуется обеспечить организацию образовательного процесса.", 11, false);
      addJustified(document, "3.2. Слушатель обязуется соблюдать правила внутреннего распорядка и сроки оплаты.", 11, false);

      addSpacer(document, 1);
      addCenteredBold(document, "4. Заключительные положения", 12);
      addJustified(document, "4.1. Договор сформирован в электронном виде и хранится в системе управления курсами.", 11, false);
      addJustified(document, "4.2. Настоящий договор вступает в силу с даты его формирования.", 11, false);

      addSpacer(document, 2);
      addLeft(document, "Исполнитель: ______________________          Слушатель: ______________________", 11);
      addLeft(document, "БНТУ                                        " + fullName, 11);

      document.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to generate contract docx", e);
    }
  }

  private static void addCenteredBold(XWPFDocument document, String text, int size) {
    XWPFParagraph p = document.createParagraph();
    p.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = p.createRun();
    run.setFontFamily(CONTRACT_FONT);
    run.setBold(true);
    run.setFontSize(size);
    run.setText(text);
  }

  private static void addCentered(XWPFDocument document, String text, int size) {
    XWPFParagraph p = document.createParagraph();
    p.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = p.createRun();
    run.setFontFamily(CONTRACT_FONT);
    run.setFontSize(size);
    run.setText(text);
  }

  private static void addLeft(XWPFDocument document, String text, int size) {
    XWPFParagraph p = document.createParagraph();
    p.setAlignment(ParagraphAlignment.LEFT);
    XWPFRun run = p.createRun();
    run.setFontFamily(CONTRACT_FONT);
    run.setFontSize(size);
    run.setText(text);
  }

  private static void addJustified(XWPFDocument document, String text, int size, boolean bold) {
    XWPFParagraph p = document.createParagraph();
    p.setAlignment(ParagraphAlignment.BOTH);
    XWPFRun run = p.createRun();
    run.setFontFamily(CONTRACT_FONT);
    run.setBold(bold);
    run.setFontSize(size);
    run.setText(text);
  }

  private static void addSpacer(XWPFDocument document, int lines) {
    for (int i = 0; i < lines; i++) {
      document.createParagraph();
    }
  }

  private static String safe(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }

  private static String buildFullName(String lastName, String firstName, String middleName) {
    if (middleName == null || middleName.isBlank()) {
      return lastName + " " + firstName;
    }
    return lastName + " " + firstName + " " + middleName;
  }

  private ContractDocumentResponse toResponse(ContractDocumentEntity e) {
    return new ContractDocumentResponse(
        e.getId(),
        e.getUserId(),
        e.getCourseId(),
        e.getGroupId(),
        e.getContractNumber(),
        e.getFileName(),
        e.getBasePrice(),
        e.getDiscountPercent(),
        e.getFinalPrice(),
        e.getCreatedAt()
    );
  }

  public record GeneratedContract(String contractNumber, byte[] content, String fileName) {
  }

  public record StoredContractFile(String fileName, org.springframework.core.io.Resource resource) {
  }
}
