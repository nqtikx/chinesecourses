package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.AdminContractCreateRequest;
import com.bntu.chinesecourses.model.dto.ContractDocumentResponse;
import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import com.bntu.chinesecourses.model.entity.ContractDocumentEntity;
import com.bntu.chinesecourses.model.entity.CourseEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.PersonGuardianEntity;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.ContractDocumentRepository;
import com.bntu.chinesecourses.repository.CourseRepository;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.PersonGuardianRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractPdfService {

  private static final String CONTRACT_FONT = "Times New Roman";
  private static final int FONT_PT = 10;   // main font size (10pt → sz=20)
  private static final int FONT_PT_SM = 8; // small captions (8pt → sz=16)
  private static final DateTimeFormatter CONTRACT_DATE_PREFIX = DateTimeFormatter.BASIC_ISO_DATE;

  private final AdminUserService adminUserService;
  private final UserProfileService userProfileService;
  private final EnrollmentService enrollmentService;
  private final EnrollmentRepository enrollmentRepository;
  private final PersonRepository personRepository;
  private final PersonGuardianRepository personGuardianRepository;
  private final CourseRepository courseRepository;
  private final SemesterRepository semesterRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final ContractDocumentRepository contractDocumentRepository;
  private final FileStorageService fileStorageService;
  private final CurrentUserService currentUserService;
  private final GroupAccessService groupAccessService;
  private final SemesterDiscountService semesterDiscountService;
  private final ContractPlaceholderResolver placeholderResolver;
  private final ContractTemplateProcessor templateProcessor;
  private final BigDecimal semesterBasePrice;
  private final Path templateDir;

  public ContractPdfService(
      AdminUserService adminUserService,
      UserProfileService userProfileService,
      EnrollmentService enrollmentService,
      EnrollmentRepository enrollmentRepository,
      PersonRepository personRepository,
      PersonGuardianRepository personGuardianRepository,
      CourseRepository courseRepository,
      SemesterRepository semesterRepository,
      StudyGroupRepository studyGroupRepository,
      ContractDocumentRepository contractDocumentRepository,
      FileStorageService fileStorageService,
      CurrentUserService currentUserService,
      GroupAccessService groupAccessService,
      SemesterDiscountService semesterDiscountService,
      ContractPlaceholderResolver placeholderResolver,
      ContractTemplateProcessor templateProcessor,
      @Value("${app.contracts.base-price:1200}") BigDecimal semesterBasePrice,
      @Value("${app.storage.templates-dir:}") String templatesDirValue) {
    this.adminUserService = adminUserService;
    this.userProfileService = userProfileService;
    this.enrollmentService = enrollmentService;
    this.enrollmentRepository = enrollmentRepository;
    this.personRepository = personRepository;
    this.personGuardianRepository = personGuardianRepository;
    this.courseRepository = courseRepository;
    this.semesterRepository = semesterRepository;
    this.studyGroupRepository = studyGroupRepository;
    this.contractDocumentRepository = contractDocumentRepository;
    this.fileStorageService = fileStorageService;
    this.currentUserService = currentUserService;
    this.groupAccessService = groupAccessService;
    this.semesterDiscountService = semesterDiscountService;
    this.placeholderResolver = placeholderResolver;
    this.templateProcessor = templateProcessor;
    this.semesterBasePrice = semesterBasePrice;
    this.templateDir = templatesDirValue == null || templatesDirValue.isBlank()
        ? null
        : Paths.get(templatesDirValue).toAbsolutePath().normalize();
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

    StudyGroupEntity group = resolveGroup(enrollment.getGroupId());
    SemesterEntity semester = semesterRepository.findById(enrollment.getSemesterId())
        .orElseThrow(() -> new NotFoundException("Semester not found id=" + enrollment.getSemesterId()));

    String contractNumber = resolveOrCreateContractNumber(enrollment);

    int discountPercent = semesterDiscountService
        .evaluate(personId, enrollment.getSemesterId())
        .nextDiscountPercent();

    PriceCalculation calculation = calculatePrice(semesterBasePrice, discountPercent);

    byte[] docxBytes = buildWord(
        contractNumber,
        person,
        personGuardianRepository.findByChildPersonIdAndArchivedFalseOrderByPrimaryGuardianDescCreatedAtAsc(person.getId()),
        course.getName(),
        LocalDate.now(),
        calculation.finalPrice(),
        calculation.discountPercent(),
        enrollment,
        semester);

    String filePath = fileStorageService.saveContract(docxBytes, "docx");
    String fileName = "contract-" + contractNumber + ".docx";
    Long generatedBy = currentUserService.getCurrentUserId().orElse(0L);

    contractDocumentRepository.save(new ContractDocumentEntity(
        null,
        enrollment.getId(),
        user.getId(),
        course.getId(),
        group == null ? null : group.getId(),
        contractNumber,
        fileName,
        filePath,
        calculation.basePrice(),
        calculation.discountPercent(),
        calculation.finalPrice(),
        generatedBy,
        Instant.now()));

    return new GeneratedContract(contractNumber, docxBytes, fileName);
  }

  /**
   * Generates a contract by processing a DOCX template file.
   */
  @Transactional
  public GeneratedContract generateFromTemplate(AdminContractCreateRequest request) {
    String rawTemplateName = request.templateName();
    if (rawTemplateName == null || rawTemplateName.isBlank()) {
      throw new IllegalArgumentException("templateName is required for template-based generation");
    }

    if (templateDir == null) {
      throw new IllegalStateException(
          "Templates directory is not configured (app.storage.templates-dir)");
    }

    String safeName = Paths.get(rawTemplateName).getFileName().toString();
    if (!safeName.toLowerCase().endsWith(".docx")) {
      throw new IllegalArgumentException("Template file must have a .docx extension");
    }

    Path templatePath = templateDir.resolve(safeName);
    if (!Files.exists(templatePath)) {
      throw new NotFoundException("Template not found: " + safeName);
    }

    Long userId = Objects.requireNonNull(request.userId(), "userId is required");
    Long courseId = Objects.requireNonNull(request.courseId(), "courseId is required");

    AdminUserEntity user = adminUserService.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found id=" + userId));

    Long personId = userProfileService.resolvePersonId(user);
    if (personId == null) {
      throw new NotFoundException("User is not linked to a person profile");
    }

    PersonEntity person = personRepository.findById(personId)
        .orElseThrow(() -> new NotFoundException("Person not found id=" + personId));

    CourseEntity course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException("Course not found id=" + courseId));

    EnrollmentEntity enrollment = enrollmentService.findForContract(personId, courseId, request.groupId());
    StudyGroupEntity group = resolveGroup(enrollment.getGroupId());

    String contractNumber = resolveOrCreateContractNumber(enrollment);

    List<PersonGuardianEntity> guardians =
        personGuardianRepository.findByChildPersonIdAndArchivedFalseOrderByPrimaryGuardianDescCreatedAtAsc(personId);

    Map<String, String> placeholders = placeholderResolver.resolve(
        person, guardians, contractNumber, course.getName(), enrollment);

    byte[] docxBytes;
    try (var stream = Files.newInputStream(templatePath)) {
      docxBytes = templateProcessor.process(stream, placeholders);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read template file: " + safeName, e);
    }

    String filePath = fileStorageService.saveContract(docxBytes, "docx");
    String fileName = "contract-" + contractNumber + ".docx";
    Long generatedBy = currentUserService.getCurrentUserId().orElse(0L);

    int discountPercent = semesterDiscountService
        .evaluate(personId, enrollment.getSemesterId())
        .nextDiscountPercent();
    PriceCalculation calculation = calculatePrice(semesterBasePrice, discountPercent);

    contractDocumentRepository.save(new ContractDocumentEntity(
        null,
        enrollment.getId(),
        user.getId(),
        course.getId(),
        group == null ? null : group.getId(),
        contractNumber,
        fileName,
        filePath,
        calculation.basePrice(),
        calculation.discountPercent(),
        calculation.finalPrice(),
        generatedBy,
        Instant.now()));

    return new GeneratedContract(contractNumber, docxBytes, fileName);
  }

  /**
   * Returns the names of {@code .docx} files available in the configured templates directory.
   */
  public List<String> listTemplates() {
    if (templateDir == null) {
      throw new IllegalStateException(
          "Templates directory is not configured (app.storage.templates-dir)");
    }
    if (!Files.isDirectory(templateDir)) {
      return List.of();
    }
    try (var paths = Files.list(templateDir)) {
      return paths
          .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".docx"))
          .map(p -> p.getFileName().toString())
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to list templates in " + templateDir, e);
    }
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

    if (currentUserService.isAdmin()) {
      return new StoredContractFile(doc.getFileName(), fileStorageService.loadAsResource(doc.getStoragePath()));
    }

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

  // ─────────────────────────────────────────────────────────────────────────────
  // Internal helpers
  // ─────────────────────────────────────────────────────────────────────────────

  private StudyGroupEntity resolveGroup(Long groupId) {
    if (groupId == null) return null;
    return studyGroupRepository.findById(groupId).orElse(null);
  }

  private String resolveOrCreateContractNumber(EnrollmentEntity enrollment) {
    String contractNumber = enrollment.getContractNumber();
    if (contractNumber != null && !contractNumber.isBlank()) return contractNumber;
    String generated = generateUniqueContractNumber();
    enrollment.setContractNumber(generated);
    return generated;
  }

  private PriceCalculation calculatePrice(BigDecimal basePrice, int discountPercent) {
    BigDecimal discount = basePrice
        .multiply(BigDecimal.valueOf(discountPercent))
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    BigDecimal finalPrice = basePrice.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    return new PriceCalculation(basePrice.setScale(2, RoundingMode.HALF_UP), discountPercent, finalPrice);
  }

  private String generateUniqueContractNumber() {
    String prefix = "CTR-" + LocalDate.now().format(CONTRACT_DATE_PREFIX) + "-";
    for (int i = 0; i < 20; i++) {
      String candidate = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
      if (!enrollmentRepository.existsByContractNumber(candidate)) return candidate;
    }
    throw new IllegalStateException("Unable to generate unique contract number");
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // DOCX generation — reproduces КЯдДиП (В) 1 семестр.docx template formatting
  // ─────────────────────────────────────────────────────────────────────────────

  private byte[] buildWord(
      String contractNumber,
      PersonEntity person,
      List<PersonGuardianEntity> guardians,
      String courseName,
      LocalDate generatedAt,
      BigDecimal finalPrice,
      int discountPercent,
      EnrollmentEntity enrollment,
      SemesterEntity semester) {

    try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      // ── Prepare data ──────────────────────────────────────────────────────────
      String fullName = buildFullName(
          person.getLastName(), person.getFirstName(), person.getMiddleName());
      PersonGuardianEntity primaryGuardian = guardians.stream()
          .filter(PersonGuardianEntity::isPrimaryGuardian)
          .findFirst()
          .orElse(guardians.isEmpty() ? null : guardians.get(0));
      String guardianFullName = primaryGuardian == null
          ? "—"
          : safe(primaryGuardian.getFullName());
      String guardianPhone = primaryGuardian == null ? "—" : safe(primaryGuardian.getPhone());
      String residentialAddress = safe(person.getResidentialAddress());
      String documentLine = buildDocumentLine(person);
      String phone = safe(person.getPhone());
      String priceFormatted = finalPrice.setScale(2, RoundingMode.HALF_UP)
          .toPlainString().replace('.', ',');
      String priceWords = amountInWords(finalPrice);
      boolean childContract = isChildContract(courseName);
      int academicHours = resolveAcademicHours(courseName);
      String year = String.valueOf(generatedAt.getYear());
      LocalDate studyStart = enrollment.getStartDate() == null
          ? semester.getStartDate() : enrollment.getStartDate();
      LocalDate studyEnd = enrollment.getEndDate() == null
          ? semester.getEndDate() : enrollment.getEndDate();
      LocalDate payEnd = studyStart.plusDays(25);
      DateTimeFormatter dmy = DateTimeFormatter.ofPattern("dd.MM.yyyy");
      String safeContractNumber = safe(contractNumber);

      // ── Title block ───────────────────────────────────────────────────────────
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.CENTER);
        cRun(p, FONT_PT, true, false, false, "ДОГОВОР №" + safeContractNumber);
      }
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.CENTER);
        cRun(p, FONT_PT, true, false, false, "о платных услугах в сфере образования в БНТУ");
      }
      // subtitle: "(" sz=10 + inner text sz=8 + ")" sz=10
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.CENTER);
        cRun(p, FONT_PT, false, false, false, "(");
        cRun(p, FONT_PT_SM, false, false, false, "на обучающих курсах по изучению китайского языка");
        cRun(p, FONT_PT, false, false, false, ")");
      }
      bodyPara(doc, ParagraphAlignment.BOTH); // blank
      // date line: "г. Минск … «___»___________YEAR г."
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false,
            "г. Минск" + " ".repeat(97) + "«___»___________" + year + " г.");
      }
      bodyPara(doc, ParagraphAlignment.BOTH); // blank

      // ── Preamble ──────────────────────────────────────────────────────────────
      String listenerWord = childContract ? "граждане" : "гражданин(ка)";
      if (childContract) {
        // Child contract: guardian line + child name (RU) + child name (BY)
        {
          XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
          XWPFRun r1 = cRun(p, FONT_PT, false, false, false,
              "Белорусский национальный технический университет (далее – БНТУ) в лице первого"
                  + " проректора Сафонова Андрея Ивановича, действующего на основании доверенности"
                  + " №01-27 / 178 от 11.01.2024, с одной стороны, и " + listenerWord);
          r1.addBreak();
          r1.addBreak();
          // Guardian full name (filled in), then label
          cRun(p, FONT_PT, false, false, false, guardianFullName + ",");
          XWPFRun r2 = p.createRun();
          r2.addBreak();
          cRun(p, FONT_PT_SM, false, false, false,
              "                                                                                    "
                  + "(ФИО законного представителя ребёнка)");
        }
        bodyPara(doc, ParagraphAlignment.BOTH);
        // Child name in Russian (filled in)
        {
          XWPFParagraph p = bodyPara(doc, ParagraphAlignment.CENTER);
          cRun(p, FONT_PT, false, false, false, fullName);
          cRun(p, FONT_PT_SM, false, false, false,
              "                                                                                               "
                  + "(ФИО ребёнка на русском языке)");
        }
        // Child name in Belarusian (blank — we don't have it)
        {
          XWPFParagraph p = bodyPara(doc, ParagraphAlignment.CENTER);
          cRun(p, FONT_PT, false, false, false,
              "________________________________________________________________________________________________________");
          cRun(p, FONT_PT_SM, false, false, false,
              "                                                                                               "
                  + "(ФИО ребёнка на белорусском языке)");
        }
        bodyPara(doc, ParagraphAlignment.CENTER);
        bodyPara(doc, ParagraphAlignment.BOTH);
        bodyPara(doc, ParagraphAlignment.BOTH);
      } else {
        // Adult contract: name on preamble paragraph, then caption as separate 8pt paragraph
        {
          XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
          XWPFRun r1 = cRun(p, FONT_PT, false, false, false,
              "Белорусский национальный технический университет (далее – БНТУ) в лице первого"
                  + " проректора Сафонова Андрея Ивановича, действующего на основании доверенности"
                  + " №01-27 / 178 от 11.01.2024, с одной стороны, и " + listenerWord);
          r1.addBreak();
          r1.addBreak();
          cRun(p, FONT_PT, false, false, false, fullName + ",");
        }
        // Caption as a separate paragraph at 8pt (matches sample)
        {
          XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
          cRun(p, FONT_PT_SM, false, false, false, "ФИО гражданина(ки))");
        }
      }
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false,
            "проживающий(ая) по адресу: " + residentialAddress + " (далее – Слушатель),"
                + " с другой стороны, заключили настоящий договор о нижеследующем:");
      }

      // ── Предмет договора ──────────────────────────────────────────────────────
      sectionHeading(doc, "Предмет договора");
      // п.1
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "1.");
        cRun(p, FONT_PT, false, false, false,
            "Освоение Слушателем образовательной программы дополнительного образования ");
        cRun(p, FONT_PT, true, true, true, "«" + safe(courseName) + "»");
        cRun(p, FONT_PT, false, false, false,
            " обучающих курсов по изучению китайского языка на платной основе в объёме "
                + academicHours + " учебных " + (academicHours == 64 ? "часа." : "часов."));
      }
      // п.2 — study dates
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "2.");
        cRun(p, FONT_PT, false, false, false, "Срок обучения: ");
        cRun(p, FONT_PT, true, true, false, "с ");
        cRun(p, FONT_PT, true, true, true,
            studyStart.format(dmy) + "      по        " + studyEnd.format(dmy));
        cRunUnderline(p, FONT_PT, false, true, ".");
      }
      // price line (no number)
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false,
            "Стоимость обучения на момент заключения настоящего договора составляет ");
        cRun(p, FONT_PT, true, true, true,
            priceFormatted + " (" + priceWords + ") белорусских рублей.");
      }
      if (discountPercent > 0) {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false,
            "Скидка по системе лояльности: " + discountPercent + "%.");
      }

      // ── Порядок изменения стоимости обучения ──────────────────────────────────
      sectionHeading(doc, "Порядок изменения стоимости обучения");
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "3.");
        cRun(p, FONT_PT, false, false, false,
            "Стоимость обучения, предусмотренная настоящим договором, может изменяться"
                + " в случаях увеличения тарифной ставки первого разряда для оплаты труда"
                + " работников бюджетных организаций, изменения условий оплаты труда,"
                + " рост тарифов на коммунальные услуги и иные ценообразующие факторы,"
                + " применяемые при формировании данной услуги. Цена может изменяться"
                + " также в случае индексации доходов населения в связи с опубликованием"
                + " индекса потребительских цен в соответствии с действующим законодательством.");
      }

      // ── Порядок расчетов за обучение ─────────────────────────────────────────
      sectionHeading(doc, "Порядок расчетов за обучение");
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "4.");
        cRun(p, FONT_PT, false, false, false,
            "Оплата за обучение на основании настоящего договора осуществляется Слушателем"
                + " на текущий (расчётный) счёт по учёту внебюджетных средств БНТУ.");
      }
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "5.");
        cRun(p, FONT_PT, false, false, false,
            "Оплата осуществляется после заключения настоящего договора в срок с    ");
        cRun(p, FONT_PT, true, true, true,
            studyStart.format(dmy) + "       по     " + payEnd.format(dmy) + ".");
      }

      // ── Права и обязанности сторон ────────────────────────────────────────────
      sectionHeading(doc, "Права и обязанности сторон");
      // п.6
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "6.");
        cRunUnderline(p, FONT_PT, false, false, "БНТУ имеет право");
        cRun(p, FONT_PT, false, false, false, ":");
      }
      simpleBodyPara(doc, "- определять самостоятельно формы, методы и способы   осуществления образовательного процесса;");
      simpleBodyPara(doc, "- досрочно прекращать образовательные отношения на основаниях, установленных в статье 79 Кодекса Республики Беларусь об образовании;");
      simpleBodyPara(doc, "- применять меры дисциплинарного взыскания к Слушателю при наличии оснований, предусмотренных в статье 126 Кодекса Республики Беларусь об образовании;");
      bodyPara(doc, ParagraphAlignment.BOTH);
      bodyPara(doc, ParagraphAlignment.BOTH);
      // п.7
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "7.");
        cRunUnderline(p, FONT_PT, false, false, "БНТУ обязуется");
        cRun(p, FONT_PT, false, false, false, ":");
      }
      simpleBodyPara(doc, "- зачислить Слушателя на обучение в соответствии с настоящим договором"
          + " и обеспечивать его подготовку при усвоении содержания образовательной программы"
          + " дополнительного образования взрослых, указанной в п.1 настоящего договора;");
      simpleBodyPara(doc, "- выдать Слушателю, освоившему содержание образовательной программы"
          + " дополнительного образования взрослых, документ об окончании курсов китайского языка.");
      // п.8
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "8.");
        cRunUnderline(p, FONT_PT, false, false, "Слушатель имеет право");
        cRun(p, FONT_PT, false, false, false, ":");
      }
      simpleBodyPara(doc, "- на получение дополнительного образования взрослых в соответствии с п.1 настоящего договора;");
      simpleBodyPara(doc, "- требовать от БНТУ оказания квалифицированных и качественных    услуг по настоящему договору;");
      simpleBodyPara(doc, "- на досрочное прекращение образовательных отношений по своей инициативе.");
      // п.9
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "9.");
        cRunUnderline(p, FONT_PT, false, false, "Слушатель обязуется");
        cRun(p, FONT_PT, false, false, false, ":");
      }
      simpleBodyPara(doc, "- добросовестно осваивать содержание образовательной программы дополнительного образования взрослых;");
      simpleBodyPara(doc, "- выполнять требования Устава БНТУ, правил внутреннего распорядка для обучающихся в БНТУ, иных локальных нормативных правовых актов БНТУ;");
      simpleBodyPara(doc, "- соблюдать правила и нормы охраны труда, пожарной безопасности, бережно относиться к имуществу БНТУ;");
      simpleBodyPara(doc, "- осуществлять оплату стоимости обучения в сроки, установленные в пункте 6 настоящего договора.");

      // ── Ответственность сторон ────────────────────────────────────────────────
      sectionHeading(doc, "Ответственность сторон");
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "10.");
        cRun(p, FONT_PT, false, false, false,
            "За неисполнение или ненадлежащее исполнение своих обязательств по настоящему"
                + " договору стороны несут ответственность в соответствии с законодательством"
                + " Республики Беларусь.");
      }
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "11.");
        cRun(p, FONT_PT, false, false, false,
            "При нарушении сроков оплаты, предусмотренных пп. 4 и 5 настоящего договора,"
                + " Слушатель выплачивает пеню в размере 0,1% от суммы просроченных платежей"
                + " за каждый день просрочки.");
      }
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "12.");
        cRun(p, FONT_PT, false, false, false,
            "Слушатель несет ответственность перед БНТУ за причинение вреда имуществу БНТУ"
                + " в соответствии с законодательством Республики Беларусь.");
      }

      // ── Заключительные положения ──────────────────────────────────────────────
      sectionHeading(doc, "Заключительные положения");
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "13.");
        cRun(p, FONT_PT, false, false, false,
            "Настоящий договор составлен в 2 экземплярах, имеющих одинаковую юридическую"
                + " силу, по одному для каждой из сторон.");
      }
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "14.");
        cRun(p, FONT_PT, false, false, false,
            "Настоящий договор вступает в силу со дня его подписания сторонами и действует"
                + " до исполнения сторонами своих обязательств.");
      }
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "15.");
        cRun(p, FONT_PT, false, false, false,
            "Вносимые изменения (дополнения) оформляются дополнительными соглашениями.");
      }
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "16.");
        cRun(p, FONT_PT, false, false, false,
            "Все споры и разногласия по настоящему договору стороны решают путем переговоров,"
                + " а при недостижении согласия – в порядке, установленном законодательством"
                + " Республики Беларусь.");
      }
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "17.");
        cRun(p, FONT_PT, false, false, false,
            "Договор изменяется и расторгается в соответствии с законодательством Республики Беларусь.");
      }
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "18.");
        cRun(p, FONT_PT, false, false, false,
            "В целях оперативности и срочности решения вопросов настоящий договор и другие"
                + " документы, касающиеся договора, могут быть изготовлены и переданы посредством"
                + " электронной и факсимильной связи, с последующим предоставлением оригиналов"
                + " в течение 10 рабочих дней. За достоверность документов и подписи стороны несут"
                + " ответственность в соответствии с действующим законодательством.");
      }
      {
        XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
        numTab(p, "19.");
        cRun(p, FONT_PT, false, false, false, "Адреса, реквизиты и подписи сторон:");
      }
      bodyPara(doc, ParagraphAlignment.BOTH);
      bodyPara(doc, ParagraphAlignment.BOTH);
      bodyPara(doc, ParagraphAlignment.BOTH);

      // ── Signature table ───────────────────────────────────────────────────────
      addSignatureTable(doc, childContract, fullName, guardianFullName,
          guardianPhone, residentialAddress, documentLine, phone);

      // ── Page / section properties ─────────────────────────────────────────────
      setupPageA4(doc);

      doc.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to generate contract docx", e);
    }
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Paragraph / run helpers
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Creates a paragraph with w:ind w:left="-1134" and the given alignment.
   * Matches the default body paragraph style of the template.
   */
  private static XWPFParagraph bodyPara(XWPFDocument doc, ParagraphAlignment align) {
    XWPFParagraph p = doc.createParagraph();
    p.setAlignment(align);
    p.setIndentationLeft(-1134);
    return p;
  }

  /** Creates a body paragraph with one normal run. */
  private static void simpleBodyPara(XWPFDocument doc, String text) {
    XWPFParagraph p = bodyPara(doc, ParagraphAlignment.BOTH);
    cRun(p, FONT_PT, false, false, false, text);
  }

  /**
   * Creates a bold, centered section heading with 1.5× line spacing (w:line=360).
   * Matches template section titles: Предмет договора, Заключительные положения, etc.
   */
  private static void sectionHeading(XWPFDocument doc, String text) {
    XWPFParagraph p = bodyPara(doc, ParagraphAlignment.CENTER);
    p.setSpacingBetween(1.5, LineSpacingRule.AUTO); // line=360
    cRun(p, FONT_PT, true, false, false, text);
  }

  /**
   * Adds a numbered-item run: "N." followed by a w:tab, matching template numbered paragraphs.
   * The caller then adds subsequent runs for the paragraph text.
   */
  private static void numTab(XWPFParagraph p, String numText) {
    XWPFRun r = p.createRun();
    r.setFontFamily(CONTRACT_FONT);
    r.setFontSize(FONT_PT);
    r.setText(numText);
    r.addTab();
  }

  /**
   * Creates a formatted run and appends it to the paragraph.
   *
   * @param size      font size in points
   * @param bold      bold flag
   * @param italic    italic flag
   * @param underline single underline flag (w:u val="single")
   */
  private static XWPFRun cRun(XWPFParagraph p, int size, boolean bold, boolean italic,
      boolean underline, String text) {
    XWPFRun r = p.createRun();
    r.setFontFamily(CONTRACT_FONT);
    r.setFontSize(size);
    r.setBold(bold);
    r.setItalic(italic);
    if (underline) r.setUnderline(UnderlinePatterns.SINGLE);
    r.setText(text);
    return r;
  }

  /** Convenience: underline-only run (not bold, not italic). */
  private static XWPFRun cRunUnderline(XWPFParagraph p, int size, boolean bold, boolean italic,
      String text) {
    XWPFRun r = p.createRun();
    r.setFontFamily(CONTRACT_FONT);
    r.setFontSize(size);
    r.setBold(bold);
    r.setItalic(italic);
    r.setUnderline(UnderlinePatterns.SINGLE);
    r.setText(text);
    return r;
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Signature table (2 columns: Исполнитель | Слушатель)
  // Table width 11475 twips, indent -1026, no visible borders
  // ─────────────────────────────────────────────────────────────────────────────

  private static void addSignatureTable(XWPFDocument doc, boolean childContract,
      String fullName, String guardianFullName, String guardianPhone,
      String residentialAddress, String documentLine, String phone) {

    XWPFTable table = doc.createTable(1, 2);

    // ── Table properties ──────────────────────────────────────────────────────
    CTTblPr tblPr = table.getCTTbl().getTblPr();

    // Remove POI's default borders
    CTTblBorders borders = tblPr.isSetTblBorders()
        ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
    CTBorder nil = CTBorder.Factory.newInstance();
    nil.setVal(STBorder.NIL);
    borders.setTop(nil);
    borders.setLeft(nil);
    borders.setBottom(nil);
    borders.setRight(nil);
    borders.setInsideH(nil);
    borders.setInsideV(nil);

    // Table width: 10817 twips (matches sample)
    CTTblWidth tblW = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
    tblW.setW(BigInteger.valueOf(10817));
    tblW.setType(STTblWidth.DXA);

    // Table indent: -1026 twips
    CTTblWidth tblInd = tblPr.isSetTblInd() ? tblPr.getTblInd() : tblPr.addNewTblInd();
    tblInd.setW(BigInteger.valueOf(-1026));
    tblInd.setType(STTblWidth.DXA);

    // Cell margins: left=10, right=10 DXA (matches sample tblCellMar)
    var tblCellMar = tblPr.isSetTblCellMar() ? tblPr.getTblCellMar() : tblPr.addNewTblCellMar();
    var cellMarLeft = tblCellMar.isSetLeft() ? tblCellMar.getLeft() : tblCellMar.addNewLeft();
    cellMarLeft.setW(BigInteger.valueOf(10));
    cellMarLeft.setType(STTblWidth.DXA);
    var cellMarRight = tblCellMar.isSetRight() ? tblCellMar.getRight() : tblCellMar.addNewRight();
    cellMarRight.setW(BigInteger.valueOf(10));
    cellMarRight.setType(STTblWidth.DXA);

    // ── Row ───────────────────────────────────────────────────────────────────
    XWPFTableRow row = table.getRow(0);

    // ── Left cell: Исполнитель (БНТУ) ─────────────────────────────────────────
    XWPFTableCell leftCell = row.getCell(0);
    setCellWidth(leftCell, 5664);
    fillExecutorCell(leftCell);

    // ── Right cell: Слушатель ─────────────────────────────────────────────────
    XWPFTableCell rightCell = row.getCell(1);
    setCellWidth(rightCell, 5153);
    fillListenerCell(rightCell, childContract, fullName, guardianFullName,
        guardianPhone, residentialAddress, documentLine, phone);
  }

  private static void setCellWidth(XWPFTableCell cell, int widthTwips) {
    CTTcPr tcPr = cell.getCTTc().isSetTcPr()
        ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    CTTblWidth tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
    tcW.setW(BigInteger.valueOf(widthTwips));
    tcW.setType(STTblWidth.DXA);
  }

  // ── Исполнитель cell content ─────────────────────────────────────────────────
  private static void fillExecutorCell(XWPFTableCell cell) {
    boolean first = true;
    // "Исполнитель:"
    {
      XWPFParagraph p = cp(cell, first, 256, ParagraphAlignment.BOTH); first = false;
      cRun(p, FONT_PT, true, false, false, "Исполнитель:");
    }
    cp(cell, false, 256, ParagraphAlignment.BOTH); // blank bold line
    // "Белорусский национальный технический университет"
    {
      XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
      cRun(p, FONT_PT, false, false, false, "Белорусский национальный технический университет");
    }
    cp(cell, false, 256, ParagraphAlignment.BOTH); // blank
    // Bank details (line=360)
    {
      XWPFParagraph p = cp(cell, false, 360, ParagraphAlignment.BOTH);
      cRun(p, FONT_PT, false, false, false, "Расчетный счет: BY69 AKBB 3632 9016 3601 3550 0000");
    }
    {
      XWPFParagraph p = cp(cell, false, 360, ParagraphAlignment.BOTH);
      cRun(p, FONT_PT, false, false, false, "ОАО «АСБ Беларусбанк»");
    }
    {
      XWPFParagraph p = cp(cell, false, 360, ParagraphAlignment.BOTH);
      cRun(p, FONT_PT, false, false, false, "БИК AKBBB Y2X г.Минск,");
    }
    {
      XWPFParagraph p = cp(cell, false, 360, ParagraphAlignment.BOTH);
      cRun(p, FONT_PT, false, false, false, "УНП 100 354 447");
    }
    {
      XWPFParagraph p = cp(cell, false, 360, ParagraphAlignment.BOTH);
      cRun(p, FONT_PT, false, false, false, "ОКПО 02 071 903");
    }
    cp(cell, false, 256, ParagraphAlignment.BOTH);
    cp(cell, false, 256, ParagraphAlignment.BOTH);
    cp(cell, false, 256, ParagraphAlignment.BOTH);
    // "Руководитель: первый проректор БНТУ"
    {
      XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
      cRun(p, FONT_PT, true, false, false, "Руководитель");
      cRun(p, FONT_PT, false, false, false, ": первый проректор БНТУ");
    }
    {
      XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
      cRun(p, FONT_PT, false, false, false, "Сафонов Андрей Иванович");
    }
    // Signature line: "_____\nМ.П. (подпись)" — directly after name, no blanks
    {
      XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
      XWPFRun r1 = cRun(p, FONT_PT, false, false, false, "_____________________________");
      r1.addBreak();
      cRun(p, FONT_PT, false, false, false, "М.П. (");
      cRun(p, FONT_PT_SM, false, false, false, "подпись");
      cRun(p, FONT_PT, false, false, false, ")");
    }
  }

  // ── Слушатель cell content ────────────────────────────────────────────────────
  private static void fillListenerCell(XWPFTableCell cell, boolean childContract,
      String fullName, String guardianFullName, String guardianPhone,
      String residentialAddress, String documentLine, String phone) {

    boolean first = true;
    // "Слушатель:"
    {
      XWPFParagraph p = cp(cell, first, 256, ParagraphAlignment.BOTH); first = false;
      cRun(p, FONT_PT, true, false, false, "Слушатель:");
    }
    cp(cell, false, 256, ParagraphAlignment.BOTH); // blank bold

    if (childContract) {
      // "ФИО ребёнка:"
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, "ФИО ребёнка:");
      }
      cp(cell, false, 256, ParagraphAlignment.BOTH);
      // filled name line
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, fullName);
      }
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, "___________________________________________________");
      }
      // "___\nФИО законного представителя ребёнка:"
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        XWPFRun r = cRun(p, FONT_PT, false, false, false, "___________________________________________________");
        r.addBreak();
        cRun(p, FONT_PT, false, false, false, "ФИО законного представителя ребёнка:");
      }
      // filled guardian name
      {
        XWPFParagraph p = cp(cell, false, 254, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, guardianFullName);
      }
      {
        XWPFParagraph p = cp(cell, false, 254, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, "___________________________________________________");
      }
      cp(cell, false, 256, ParagraphAlignment.BOTH);
      // "Адрес проживания ребёнка: {addr}"
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false,
            "Адрес проживания ребёнка: " + residentialAddress);
      }
      cp(cell, false, 256, ParagraphAlignment.BOTH);
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, "___________________________________________________");
      }
      // Document label (mixed sizes)
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false,
            "Документ, удостоверяющий личность законного представителя ребёнка ");
        cRun(p, FONT_PT_SM, false, false, false, "(вид, серия)");
        cRun(p, FONT_PT, false, false, false,
            ", номер, дата выдачи, наименование государственного органа, его выдавшего,"
                + " идентификационный номер ");
        cRun(p, FONT_PT_SM, false, false, false, "(при наличии");
        cRun(p, FONT_PT, false, false, false, "):");
      }
      // Filled document info
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, documentLine);
      }
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, "___________________________________________________");
      }
      cp(cell, false, 256, ParagraphAlignment.BOTH);
      cp(cell, false, 256, ParagraphAlignment.BOTH);
      // Phone line
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false,
            "Моб. телефон: законного представителя ребёнка " + guardianPhone
                + " и ребёнка " + phone + ".");
      }
      cp(cell, false, 256, ParagraphAlignment.BOTH);
      cp(cell, false, 256, ParagraphAlignment.BOTH);
      // Signature line
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        XWPFRun r = cRun(p, FONT_PT, false, false, false, "________________________________ ");
        r.addBreak();
        cRun(p, FONT_PT_SM, false, false, false, "(подпись законного представителя ребёнка)");
      }
    } else {
      // Adult contract — Слушатель column
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, "ФИО: " + fullName);
      }
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, "___________________________________________________");
      }
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, "Адрес проживания: " + residentialAddress);
      }
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, "___________________________________________________");
      }
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false,
            "Документ, удостоверяющий личность ");
        cRun(p, FONT_PT_SM, false, false, false, "(вид, серия)");
        cRun(p, FONT_PT, false, false, false,
            ", номер, дата выдачи, наименование государственного органа, его выдавшего,"
                + " идентификационный номер ");
        cRun(p, FONT_PT_SM, false, false, false, "(при наличии");
        cRun(p, FONT_PT, false, false, false, "):");
      }
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, documentLine);
      }
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, "___________________________________________________");
      }
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        cRun(p, FONT_PT, false, false, false, "Моб. телефон: " + phone);
      }
      cp(cell, false, 256, ParagraphAlignment.BOTH);
      cp(cell, false, 256, ParagraphAlignment.BOTH);
      // Paragraph with just a line break (matches sample structure before signature)
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        p.createRun().addBreak();
      }
      {
        XWPFParagraph p = cp(cell, false, 256, ParagraphAlignment.BOTH);
        XWPFRun r = cRun(p, FONT_PT, false, false, false, "_____________________________");
        r.addBreak();
        cRun(p, FONT_PT_SM, false, false, false, "(подпись)");
      }
    }
  }

  /**
   * Creates or retrieves a cell paragraph with the given line spacing and alignment.
   * Table cell paragraphs do NOT use the -1134 left indent of body paragraphs.
   *
   * @param first true → configure the first existing paragraph; false → add a new one
   * @param line  line spacing in twips (240=single, 256, 360=1.5×)
   */
  private static XWPFParagraph cp(XWPFTableCell cell, boolean first, int line,
      ParagraphAlignment align) {
    XWPFParagraph p = first ? cell.getParagraphs().get(0) : cell.addParagraph();
    p.setAlignment(align);
    p.setSpacingBetween(line / 240.0, LineSpacingRule.AUTO);
    return p;
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Page setup — A4, БНТУ-specific margins matching the template
  // top=1134, right=850, bottom=709, left=1701 (all in twips)
  // ─────────────────────────────────────────────────────────────────────────────

  private static void setupPageA4(XWPFDocument doc) {
    CTSectPr sectPr = doc.getDocument().getBody().isSetSectPr()
        ? doc.getDocument().getBody().getSectPr()
        : doc.getDocument().getBody().addNewSectPr();
    CTPageSz pgSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
    pgSz.setW(BigInteger.valueOf(11906));
    pgSz.setH(BigInteger.valueOf(16838));
    CTPageMar pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
    pgMar.setTop(BigInteger.valueOf(1134));
    pgMar.setRight(BigInteger.valueOf(850));
    pgMar.setBottom(BigInteger.valueOf(709));
    pgMar.setLeft(BigInteger.valueOf(1701));
    pgMar.setHeader(BigInteger.valueOf(708));
    pgMar.setFooter(BigInteger.valueOf(708));
    pgMar.setGutter(BigInteger.valueOf(0));
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Data helpers
  // ─────────────────────────────────────────────────────────────────────────────

  private static boolean isChildContract(String courseName) {
    String n = safe(courseName).toLowerCase();
    return n.contains("дет") || n.contains("подрост");
  }

  private static int resolveAcademicHours(String courseName) {
    return safe(courseName).toLowerCase().contains("техническ") ? 96 : 64;
  }

  private static String buildDocumentLine(PersonEntity person) {
    String type = safe(person.getDocumentType());
    String series = safe(person.getDocumentSeries());
    String number = safe(person.getDocumentNumber());
    String issueDate = person.getDocumentIssueDate() == null
        ? "-" : person.getDocumentIssueDate().toString();
    String issuedBy = safe(person.getDocumentIssuedBy());
    String idNum = safe(person.getDocumentIdentificationNumber());
    return type + ", серия " + series + ", № " + number + ", дата выдачи " + issueDate
        + ", выдан " + issuedBy + ", идентификационный номер " + idNum + ".";
  }

  private static String safe(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }

  private static String amountInWords(BigDecimal amount) {
    BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
    long rub = scaled.longValue();
    int kop = scaled.remainder(BigDecimal.ONE).movePointRight(2).intValue();
    return numberToWordsRu(rub) + " " + rubWord(rub) + ", " + String.format("%02d", kop) + " коп.";
  }

  private static String rubWord(long n) {
    long m = n % 100;
    if (m >= 11 && m <= 14) return "рублей";
    return switch ((int) (n % 10)) {
      case 1 -> "рубль";
      case 2, 3, 4 -> "рубля";
      default -> "рублей";
    };
  }

  private static String numberToWordsRu(long n) {
    String[] units = {"", "один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"};
    String[] teens = {"десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать",
        "пятнадцать", "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать"};
    String[] tens = {"", "", "двадцать", "тридцать", "сорок", "пятьдесят",
        "шестьдесят", "семьдесят", "восемьдесят", "девяносто"};
    String[] hundreds = {"", "сто", "двести", "триста", "четыреста", "пятьсот",
        "шестьсот", "семьсот", "восемьсот", "девятьсот"};
    if (n == 0) return "ноль";
    if (n >= 1000) return Long.toString(n);
    int h = (int) (n / 100);
    int t = (int) ((n % 100) / 10);
    int u = (int) (n % 10);
    if (t == 1) return (hundreds[h] + " " + teens[u]).trim();
    return (hundreds[h] + " " + tens[t] + " " + units[u]).trim().replaceAll("\\s+", " ");
  }

  private static String buildFullName(String lastName, String firstName, String middleName) {
    String ln = safe(lastName);
    String fn = safe(firstName);
    if (middleName == null || middleName.isBlank()) return (ln + " " + fn).trim();
    return (ln + " " + fn + " " + middleName).trim();
  }

  private ContractDocumentResponse toResponse(ContractDocumentEntity entity) {
    return new ContractDocumentResponse(
        entity.getId(),
        entity.getUserId(),
        entity.getCourseId(),
        entity.getGroupId(),
        entity.getContractNumber(),
        entity.getFileName(),
        entity.getBasePrice(),
        entity.getDiscountPercent(),
        entity.getFinalPrice(),
        entity.getCreatedAt());
  }

  private record PriceCalculation(BigDecimal basePrice, int discountPercent, BigDecimal finalPrice) {}

  public record GeneratedContract(String contractNumber, byte[] content, String fileName) {}

  public record StoredContractFile(String fileName, org.springframework.core.io.Resource resource) {}
}
