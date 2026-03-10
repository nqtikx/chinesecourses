package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.AdminContractCreateRequest;
import com.bntu.chinesecourses.model.dto.ContractDocumentResponse;
import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import com.bntu.chinesecourses.model.entity.ContractDocumentEntity;
import com.bntu.chinesecourses.model.entity.CourseEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.ContractDocumentRepository;
import com.bntu.chinesecourses.repository.CourseRepository;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
  private static final int BODY_FONT_SIZE = 12;
  private static final int HEADER_FONT_SIZE = 14;
  private static final int RED_LINE_TWIPS = 720; // 1.25 cm
  private static final DateTimeFormatter CONTRACT_DATE_PREFIX = DateTimeFormatter.BASIC_ISO_DATE;

  private final AdminUserService adminUserService;
  private final UserProfileService userProfileService;
  private final EnrollmentService enrollmentService;
  private final EnrollmentRepository enrollmentRepository;
  private final PersonRepository personRepository;
  private final CourseRepository courseRepository;
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
      StudyGroupRepository studyGroupRepository,
      ContractDocumentRepository contractDocumentRepository,
      FileStorageService fileStorageService,
      CurrentUserService currentUserService,
      GroupAccessService groupAccessService,
      @Value("${app.contracts.repeat-discount-percent:10}") int repeatDiscountPercent,
      @Value("${app.contracts.base-price:1200}") BigDecimal semesterBasePrice) {
    this.adminUserService = adminUserService;
    this.userProfileService = userProfileService;
    this.enrollmentService = enrollmentService;
    this.enrollmentRepository = enrollmentRepository;
    this.personRepository = personRepository;
    this.courseRepository = courseRepository;
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

    StudyGroupEntity group = resolveGroup(enrollment.getGroupId());

    String contractNumber = resolveOrCreateContractNumber(enrollment);

    int discountPercent = enrollmentService.findCompletedForStudent(personId).isEmpty()
        ? 0
        : repeatDiscountPercent;

    PriceCalculation calculation = calculatePrice(semesterBasePrice, discountPercent);

    byte[] docxBytes = buildWord(
        contractNumber,
        person,
        course.getName(),
        LocalDate.now(),
        calculation.finalPrice());

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

  private StudyGroupEntity resolveGroup(Long groupId) {
    if (groupId == null) {
      return null;
    }
    return studyGroupRepository.findById(groupId).orElse(null);
  }

  private String resolveOrCreateContractNumber(EnrollmentEntity enrollment) {
    String contractNumber = enrollment.getContractNumber();
    if (contractNumber != null && !contractNumber.isBlank()) {
      return contractNumber;
    }

    String generated = generateUniqueContractNumber();
    enrollment.setContractNumber(generated);
    return generated;
  }

  private PriceCalculation calculatePrice(BigDecimal basePrice, int discountPercent) {
    BigDecimal discount = basePrice
        .multiply(BigDecimal.valueOf(discountPercent))
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

    BigDecimal finalPrice = basePrice.subtract(discount).setScale(2, RoundingMode.HALF_UP);

    return new PriceCalculation(
        basePrice.setScale(2, RoundingMode.HALF_UP),
        discountPercent,
        finalPrice);
  }

  private String generateUniqueContractNumber() {
    String prefix = "CTR-" + LocalDate.now().format(CONTRACT_DATE_PREFIX) + "-";
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
      LocalDate generatedAt,
      BigDecimal finalPrice) {
    try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      String fullName = safe(buildFullName(person.getLastName(), person.getFirstName(), person.getMiddleName()));
      String phone = safe(person.getPhone());
      String priceValue = finalPrice.setScale(2, RoundingMode.HALF_UP).toPlainString();
      boolean childContract = isChildContract(courseName);
      int academicHours = resolveAcademicHours(courseName);
      String year = String.valueOf(generatedAt.getYear());

      addParagraph(document, "ДОГОВОР №" + safe(contractNumber), ParagraphAlignment.CENTER, HEADER_FONT_SIZE, true, 0);
      addParagraph(document, "о платных услугах в сфере образования в БНТУ", ParagraphAlignment.CENTER, BODY_FONT_SIZE, true, 0);
      addParagraph(document, "(на обучающих курсах по изучению китайского языка)", ParagraphAlignment.CENTER, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "г. Минск «___»___________" + year + " г.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, 0);

      String listenerWord = childContract ? "граждане" : "гражданин(ка)";
      addParagraph(
          document,
          "Белорусский национальный технический университет (далее – БНТУ) в лице первого проректора Сафонова Андрея Ивановича, действующего на основании доверенности №01-27 / 178 от 11.01.2024, с одной стороны, и "
              + listenerWord,
          ParagraphAlignment.BOTH,
          BODY_FONT_SIZE,
          false,
          RED_LINE_TWIPS);
      addParagraph(document, "________________________________________________________________________________________________________,", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      if (childContract) {
        addParagraph(document, "(ФИО законного представителя ребёнка)", ParagraphAlignment.CENTER, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "________________________________________________________________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "(ФИО ребёнка на русском языке)", ParagraphAlignment.CENTER, BODY_FONT_SIZE, false, 0);
        addParagraph(document, fullName, ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "________________________________________________________________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "(ФИО ребёнка на белорусском языке)", ParagraphAlignment.CENTER, BODY_FONT_SIZE, false, 0);
      } else {
        addParagraph(document, "(ФИО гражданина(ки))", ParagraphAlignment.CENTER, BODY_FONT_SIZE, false, 0);
        addParagraph(document, fullName, ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "________________________________________________________________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "(ФИО гражданина(ки) на белорусском языке))", ParagraphAlignment.CENTER, BODY_FONT_SIZE, false, 0);
      }
      addParagraph(document, "проживающий(ая) по адресу:__________________________________________________________(далее – Слушатель), с другой стороны, заключили настоящий договор о нижеследующем:", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);

      addParagraph(document, "Предмет договора", ParagraphAlignment.CENTER, BODY_FONT_SIZE, true, 0);
      addParagraph(document, "1. Освоение Слушателем образовательной программы дополнительного образования \"" + safe(courseName) + "\" обучающих курсов по изучению китайского языка на платной основе в объёме " + academicHours + " учебных " + (academicHours == 64 ? "часа." : "часов."), ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "2. Срок обучения: с 15.09.2025 по 12.01.2026.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "Стоимость обучения на момент заключения настоящего договора составляет " + priceValue + " (__________________________) белорусских рублей.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);

      addParagraph(document, "Порядок изменения стоимости обучения", ParagraphAlignment.CENTER, BODY_FONT_SIZE, true, 0);
      addParagraph(document, "3. Стоимость обучения, предусмотренная настоящим договором, может изменяться в случаях увеличения тарифной ставки первого разряда для оплаты труда работников бюджетных организаций, изменения условий оплаты труда, рост тарифов на коммунальные услуги и иные ценообразующие факторы, применяемые при формировании данной услуги. Цена может изменяться также в случае индексации доходов населения в связи с опубликованием индекса потребительских цен в соответствии с действующим законодательством.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);

      addParagraph(document, "Порядок расчетов за обучение", ParagraphAlignment.CENTER, BODY_FONT_SIZE, true, 0);
      addParagraph(document, "4. Оплата за обучение на основании настоящего договора осуществляется Слушателем на текущий (расчётный) счёт по учёту внебюджетных средств БНТУ.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "5. Оплата осуществляется после заключения настоящего договора в срок с 15.09.2025 по 10.10.2025.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);

      addParagraph(document, "Права и обязанности сторон", ParagraphAlignment.CENTER, BODY_FONT_SIZE, true, 0);
      addParagraph(document, "6. БНТУ имеет право:", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- определять самостоятельно формы, методы и способы осуществления образовательного процесса;", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- досрочно прекращать образовательные отношения на основаниях, установленных в статье 79 Кодекса Республики Беларусь об образовании;", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- применять меры дисциплинарного взыскания к Слушателю при наличии оснований, предусмотренных в статье 126 Кодекса Республики Беларусь об образовании;", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "7. БНТУ обязуется:", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- зачислить Слушателя на обучение в соответствии с настоящим договором и обеспечивать его подготовку при усвоении содержания образовательной программы дополнительного образования взрослых, указанной в п.1 настоящего договора;", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- выдать Слушателю, освоившему содержание образовательной программы дополнительного образования взрослых, документ об окончании курсов китайского языка.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "8. Слушатель имеет право:", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- на получение дополнительного образования взрослых в соответствии с п.1 настоящего договора;", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- требовать от БНТУ оказания квалифицированных и качественных услуг по настоящему договору;", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- на досрочное прекращение образовательных отношений по своей инициативе.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "9. Слушатель обязуется:", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- добросовестно осваивать содержание образовательной программы дополнительного образования взрослых;", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- выполнять требования Устава БНТУ, правил внутреннего распорядка для обучающихся в БНТУ, иных локальных нормативных правовых актов БНТУ;", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- соблюдать правила и нормы охраны труда, пожарной безопасности, бережно относиться к имуществу БНТУ;", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "- осуществлять оплату стоимости обучения в сроки, установленные в пункте 6 настоящего договора.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);

      addParagraph(document, "Ответственность сторон", ParagraphAlignment.CENTER, BODY_FONT_SIZE, true, 0);
      addParagraph(document, "10. За неисполнение или ненадлежащее исполнение своих обязательств по настоящему договору стороны несут ответственность в соответствии с законодательством Республики Беларусь.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "11. При нарушении сроков оплаты, предусмотренных пп. 4 и 5 настоящего договора, Слушатель выплачивает пеню в размере 0,1% от суммы просроченных платежей за каждый день просрочки.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "12. Слушатель несет ответственность перед БНТУ за причинение вреда имуществу БНТУ в соответствии с законодательством Республики Беларусь.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);

      addParagraph(document, "Заключительные положения", ParagraphAlignment.CENTER, BODY_FONT_SIZE, true, 0);
      addParagraph(document, "13. Настоящий договор составлен в 2 экземплярах, имеющих одинаковую юридическую силу, по одному для каждой из сторон.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "14. Настоящий договор вступает в силу со дня его подписания сторонами и действует до исполнения сторонами своих обязательств.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "15. Вносимые изменения (дополнения) оформляются дополнительными соглашениями.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "16. Все споры и разногласия по настоящему договору стороны решают путем переговоров, а при недостижении согласия – в порядке, установленном законодательством Республики Беларусь.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "17. Договор изменяется и расторгается в соответствии с законодательством Республики Беларусь.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "18. В целях оперативности и срочности решения вопросов настоящий договор и другие документы, касающиеся договора, могут быть изготовлены и переданы посредством электронной и факсимильной связи, с последующим предоставлением оригиналов в течение 10 рабочих дней. За достоверность документов и подписи стороны несут ответственность в соответствии с действующим законодательством.", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);
      addParagraph(document, "19. Адреса, реквизиты и подписи сторон:", ParagraphAlignment.BOTH, BODY_FONT_SIZE, false, RED_LINE_TWIPS);

      addParagraph(document, "Исполнитель:", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "Белорусский национальный технический университет", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "Расчетный счет: BY69 AKBB 3632 9016 3601 3550 0000", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "ОАО «АСБ Беларусбанк»", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "БИК AKBBB Y2X г.Минск,", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "УНП 100 354 447", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "ОКПО 02 071 903", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "Руководитель: первый проректор БНТУ", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "Сафонов Андрей Иванович", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "_____________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "М.П. (подпись)", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      addParagraph(document, "Слушатель:", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      if (childContract) {
        addParagraph(document, "ФИО ребёнка:", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, fullName, ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "___________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "ФИО законного представителя ребёнка:", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "___________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "___________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "Адрес проживания ребёнка:___________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "___________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "Документ, удостоверяющий личность законного представителя ребёнка (вид, серия), номер, дата выдачи, наименование государственного органа, его выдавшего, идентификационный номер (при наличии):", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "___________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "___________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "Моб. телефон: законного представителя ребёнка ___________________ и ребёнка " + phone + ".", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "(подпись законного представителя ребёнка)", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      } else {
        addParagraph(document, "ФИО:______________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, fullName, ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "________________________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "Адрес проживания:__________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "___________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "Документ, удостоверяющий личность (вид, серия (при наличии), номер, дата выдачи, наименование государственного органа, его выдавшего, идентификационный номер (при наличии):", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "___________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "___________________________________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "Моб. телефон " + phone, ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "_____________________________", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
        addParagraph(document, "(подпись)", ParagraphAlignment.LEFT, BODY_FONT_SIZE, false, 0);
      }

      document.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to generate contract docx", e);
    }
  }

  private static void addParagraph(
      XWPFDocument document,
      String text,
      ParagraphAlignment alignment,
      int fontSize,
      boolean bold,
      int firstLineIndent) {
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setAlignment(alignment);
    paragraph.setSpacingBefore(0);
    paragraph.setSpacingAfter(80);
    paragraph.setSpacingBetween(1.0);
    if (firstLineIndent > 0) {
      paragraph.setIndentationFirstLine(firstLineIndent);
    }

    XWPFRun run = paragraph.createRun();
    run.setFontFamily(CONTRACT_FONT);
    run.setFontSize(fontSize);
    run.setBold(bold);
    run.setText(text);
  }

  private static boolean isChildContract(String courseName) {
    String normalized = safe(courseName).toLowerCase();
    return normalized.contains("дет") || normalized.contains("подрост");
  }

  private static int resolveAcademicHours(String courseName) {
    String normalized = safe(courseName).toLowerCase();
    return normalized.contains("техническ") ? 96 : 64;
  }

  private static String safe(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }

  private static String buildFullName(String lastName, String firstName, String middleName) {
    String ln = safe(lastName);
    String fn = safe(firstName);

    if (middleName == null || middleName.isBlank()) {
      return (ln + " " + fn).trim();
    }

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

  private record PriceCalculation(BigDecimal basePrice, int discountPercent, BigDecimal finalPrice) {
  }

  public record GeneratedContract(String contractNumber, byte[] content, String fileName) {
  }

  public record StoredContractFile(String fileName, org.springframework.core.io.Resource resource) {
  }
}