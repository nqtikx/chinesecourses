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
      String dateYear = "2025";
      String studyStart = "15.09.2025";
      String studyEnd = "12.01.2026";
      String payStart = "15.09.2025";
      String payEnd = "10.10.2025";
      String priceText = finalPrice.setScale(2, RoundingMode.HALF_UP).toPlainString()
          + " (триста тридцать рублей, 00 коп.) белорусских рублей.";

      addCenteredBold(document, "ДОГОВОР № " + safe(contractNumber), 14);
      addCenteredBold(document, "о платных услугах в сфере образования в БНТУ", 12);
      addCentered(document, "(на обучающих курсах по изучению китайского языка)", 11);
      addSpacer(document, 1);

      addLeft(document,
          "г. Минск                                                                                                                       «___»___________" + dateYear + " г.",
          11);
      addSpacer(document, 1);

      addJustified(document,
          "Белорусский национальный технический университет (далее – БНТУ) в лице первого проректора Сафонова Андрея "
              + "Ивановича, действующего на основании доверенности №01-27 / 178 от 11.01.2024, с одной стороны, и граждане",
          12, false);
      addSpacer(document, 1);
      addLeft(document, "________________________________________________________________________________________________________,", 11);
      addCentered(document, "(ФИО законного представителя ребёнка)", 10);
      addSpacer(document, 1);
      addLeft(document, "________________________________________________________________________________________________________", 11);
      addCentered(document, "(ФИО ребёнка на русском языке)", 10);
      addSpacer(document, 1);
      addLeft(document, "________________________________________________________________________________________________________", 11);
      addCentered(document, "(ФИО ребёнка на белорусском языке)", 10);
      addSpacer(document, 1);
      addJustified(document,
          "проживающий(ая) по адресу:__________________________________________________________(далее – Слушатель), "
              + "с другой стороны, заключили настоящий договор о нижеследующем:",
          12, false);
      addSpacer(document, 1);

      addCenteredBold(document, "Предмет договора", 12);
      addJustified(document,
          "1.\tОсвоение Слушателем образовательной программы дополнительного образования «Китайский язык для детей и "
              + "подростков» обучающих курсов по изучению китайского языка на платной основе в объёме 64 учебных часа.",
          11, false);
      addJustified(document, "2.\tСрок обучения: с " + studyStart + "      по        " + studyEnd + ".", 11, false);
      addJustified(document, "Стоимость обучения на момент заключения настоящего договора составляет " + priceText, 11, false);
      addSpacer(document, 1);

      addCenteredBold(document, "Порядок изменения стоимости обучения", 12);
      addJustified(document,
          "3.\tСтоимость обучения, предусмотренная настоящим договором, может изменяться в случаях увеличения тарифной "
              + "ставки первого разряда для оплаты труда работников бюджетных организаций, изменения условий оплаты труда, рост "
              + "тарифов на коммунальные услуги и иные ценообразующие факторы, применяемые при формировании данной услуги. "
              + "Цена может изменяться также в случае индексации доходов населения в связи с опубликованием индекса "
              + "потребительских цен в соответствии с действующим законодательством.",
          11, false);
      addSpacer(document, 1);

      addCenteredBold(document, "Порядок расчетов за обучение", 12);
      addJustified(document,
          "4.\tОплата за обучение на основании настоящего договора осуществляется Слушателем на текущий (расчётный) "
              + "счёт по учёту внебюджетных средств БНТУ.",
          11, false);
      addJustified(document,
          "5.\tОплата осуществляется после заключения настоящего договора в срок с    "
              + payStart + "       по     " + payEnd + ".",
          11, false);
      addSpacer(document, 1);

      addCenteredBold(document, "Права и обязанности сторон", 12);
      addJustified(document, "6.\tБНТУ имеет право:", 11, false);
      addJustified(document,
          "- определять самостоятельно формы, методы и способы   осуществления образовательного процесса;\n"
              + "- досрочно прекращать образовательные отношения на основаниях, установленных в статье 79 Кодекса Республики Беларусь об образовании;\n"
              + "- применять меры дисциплинарного взыскания к Слушателю при наличии оснований, предусмотренных в статье 126 Кодекса Республики Беларусь об образовании;",
          11, false);
      addSpacer(document, 1);
      addJustified(document, "7.\tБНТУ обязуется:", 11, false);
      addJustified(document,
          "- зачислить Слушателя на обучение в соответствии с настоящим договором и обеспечивать его подготовку при усвоении содержания образовательной программы дополнительного образования взрослых, указанной в п.1 настоящего договора;\n"
              + "- выдать Слушателю, освоившему содержание образовательной программы дополнительного образования взрослых, документ об окончании курсов китайского языка.",
          11, false);
      addSpacer(document, 1);
      addJustified(document, "8.\tСлушатель имеет право:", 11, false);
      addJustified(document,
          "- на получение дополнительного образования взрослых в соответствии с п.1 настоящего договора;\n"
              + "- требовать от БНТУ оказания квалифицированных и качественных    услуг по настоящему договору;\n"
              + "- на досрочное прекращение образовательных отношений по своей инициативе.",
          11, false);
      addSpacer(document, 1);
      addJustified(document, "9.\tСлушатель обязуется:", 11, false);
      addJustified(document,
          "- добросовестно осваивать содержание образовательной программы дополнительного образования взрослых;\n"
              + "- выполнять требования Устава БНТУ, правил внутреннего распорядка для обучающихся в БНТУ, иных локальных нормативных правовых актов БНТУ;\n"
              + "- соблюдать правила и нормы охраны труда, пожарной безопасности, бережно относиться к имуществу БНТУ;\n"
              + "- осуществлять оплату стоимости обучения в сроки, установленные в пункте 6 настоящего договора.",
          11, false);
      addSpacer(document, 1);

      addCenteredBold(document, "Ответственность сторон", 12);
      addJustified(document,
          "10.\tЗа неисполнение или ненадлежащее исполнение своих обязательств по настоящему договору стороны несут "
              + "ответственность в соответствии с законодательством Республики Беларусь.",
          11, false);
      addJustified(document,
          "11.\tПри нарушении сроков оплаты, предусмотренных пп. 4 и 5 настоящего договора, Слушатель выплачивает пеню "
              + "в размере 0,1% от суммы просроченных платежей за каждый день просрочки.",
          11, false);
      addJustified(document,
          "12.\tСлушатель несет ответственность перед БНТУ за причинение вреда имуществу БНТУ в соответствии с "
              + "законодательством Республики Беларусь.",
          11, false);
      addSpacer(document, 1);

      addCenteredBold(document, "Заключительные положения", 12);
      addJustified(document,
          "13.\tНастоящий договор составлен в 2 экземплярах, имеющих одинаковую юридическую силу, по одному для каждой из сторон.\n"
              + "14.\tНастоящий договор вступает в силу со дня его подписания сторонами и действует до исполнения сторонами своих обязательств.\n"
              + "15.\tВносимые изменения (дополнения) оформляются дополнительными соглашениями.\n"
              + "16.\tВсе споры и разногласия по настоящему договору стороны решают путем переговоров, а при недостижении согласия – в порядке, установленном законодательством Республики Беларусь.\n"
              + "17.\tДоговор изменяется и расторгается в соответствии с законодательством Республики Беларусь.\n"
              + "18.\tВ целях оперативности и срочности решения вопросов настоящий договор и другие документы, касающиеся договора, могут быть изготовлены и переданы посредством электронной и факсимильной связи, с последующим предоставлением оригиналов в течение 10 рабочих дней. За достоверность документов и подписи стороны несут ответственность в соответствии с действующим законодательством.\n"
              + "19.\tАдреса, реквизиты и подписи сторон:",
          11, false);

      addSpacer(document, 2);
      addLeft(document, "Исполнитель:\t\t\t\t\t\t\t\tСлушатель:", 11);
      addSpacer(document, 1);
      addLeft(document, "Белорусский национальный технический университет", 11);
      addSpacer(document, 1);
      addLeft(document, "Расчетный счет: BY69 AKBB 3632 9016 3601 3550 0000", 11);
      addLeft(document, "ОАО «АСБ Беларусбанк»", 11);
      addLeft(document, "БИК AKBBB Y2X г.Минск,", 11);
      addLeft(document, "УНП 100 354 447", 11);
      addLeft(document, "ОКПО 02 071 903", 11);
      addSpacer(document, 2);
      addLeft(document, "Руководитель: первый проректор БНТУ", 11);
      addLeft(document, "Сафонов Андрей Иванович", 11);
      addSpacer(document, 2);
      addLeft(document, "_____________________________", 11);
      addLeft(document, "             М.П. (подпись)\t\t\t\t\t\t\t\tФИО ребёнка:", 11);
      addLeft(document, "___________________________________________________", 11);
      addLeft(document, "___________________________________________________", 11);
      addLeft(document, "ФИО законного представителя ребёнка:", 11);
      addLeft(document, "___________________________________________________", 11);
      addLeft(document, "___________________________________________________", 11);
      addLeft(document, "Адрес проживания ребёнка:___________________________", 11);
      addLeft(document, "___________________________________________________", 11);
      addLeft(document, "Документ, удостоверяющий личность законного представителя ребёнка (вид, серия), номер, дата выдачи,", 11);
      addLeft(document, "наименование государственного органа, его выдавшего, идентификационный номер (при наличии):", 11);
      addLeft(document, "___________________________________________________", 11);
      addLeft(document, "___________________________________________________", 11);
      addLeft(document, "Моб. телефон: законного представителя ребёнка ___________________ и ребёнка_______________________.", 11);
      addSpacer(document, 1);
      addLeft(document, "________________________________", 11);
      addLeft(document, "(подпись законного представителя ребёнка)", 11);

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
