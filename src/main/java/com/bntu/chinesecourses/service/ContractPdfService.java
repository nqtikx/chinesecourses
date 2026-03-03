package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.AdminContractCreateRequest;
import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import com.bntu.chinesecourses.model.entity.CourseEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.PersonEntity;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.CourseRepository;
import com.bntu.chinesecourses.repository.EnrollmentRepository;
import com.bntu.chinesecourses.repository.PersonRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import com.bntu.chinesecourses.repository.StudyGroupRepository;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractPdfService {

  private final AdminUserService adminUserService;
  private final UserProfileService userProfileService;
  private final EnrollmentService enrollmentService;
  private final EnrollmentRepository enrollmentRepository;
  private final PersonRepository personRepository;
  private final CourseRepository courseRepository;
  private final SemesterRepository semesterRepository;
  private final StudyGroupRepository studyGroupRepository;

  public ContractPdfService(
      AdminUserService adminUserService,
      UserProfileService userProfileService,
      EnrollmentService enrollmentService,
      EnrollmentRepository enrollmentRepository,
      PersonRepository personRepository,
      CourseRepository courseRepository,
      SemesterRepository semesterRepository,
      StudyGroupRepository studyGroupRepository
  ) {
    this.adminUserService = adminUserService;
    this.userProfileService = userProfileService;
    this.enrollmentService = enrollmentService;
    this.enrollmentRepository = enrollmentRepository;
    this.personRepository = personRepository;
    this.courseRepository = courseRepository;
    this.semesterRepository = semesterRepository;
    this.studyGroupRepository = studyGroupRepository;
  }

  @Transactional
  public GeneratedContract generate(AdminContractCreateRequest request) {
    AdminUserEntity user = adminUserService.findById(request.userId())
        .orElseThrow(() -> new NotFoundException("User not found id=" + request.userId()));
    Long personId = userProfileService.resolvePersonId(user);
    if (personId == null) {
      throw new NotFoundException("User is not linked to person profile");
    }
    PersonEntity person = personRepository.findById(personId)
        .orElseThrow(() -> new NotFoundException("Person not found id=" + personId));
    CourseEntity course = courseRepository.findById(request.courseId())
        .orElseThrow(() -> new NotFoundException("Course not found id=" + request.courseId()));

    EnrollmentEntity enrollment = enrollmentService.findForContract(personId, request.courseId(), request.groupId());
    SemesterEntity semester = semesterRepository.findById(enrollment.getSemesterId())
        .orElseThrow(() -> new NotFoundException("Semester not found id=" + enrollment.getSemesterId()));
    StudyGroupEntity group = enrollment.getGroupId() == null ? null : studyGroupRepository.findById(enrollment.getGroupId()).orElse(null);

    String contractNumber = enrollment.getContractNumber();
    if (contractNumber == null || contractNumber.isBlank()) {
      contractNumber = generateUniqueContractNumber();
      enrollment.setContractNumber(contractNumber);
    }

    byte[] pdfBytes = buildPdf(
        contractNumber,
        person,
        course.getName(),
        group == null ? null : group.getName(),
        group == null || group.getTeacher() == null || group.getTeacher().getPerson() == null
            ? null
            : buildFullName(
                group.getTeacher().getPerson().getLastName(),
                group.getTeacher().getPerson().getFirstName(),
                group.getTeacher().getPerson().getMiddleName()),
        LocalDate.now(),
        semester.getName()
    );

    return new GeneratedContract(contractNumber, pdfBytes);
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

  private byte[] buildPdf(
      String contractNumber,
      PersonEntity person,
      String courseName,
      String groupName,
      String teacherName,
      LocalDate generatedAt,
      String semesterName
  ) {
    try (PDDocument document = new PDDocument();
         ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PDPage page = new PDPage();
      document.addPage(page);
      PDFont titleFont = resolveUnicodeFont(document);
      PDFont bodyFont = titleFont;
      boolean unicode = !(titleFont instanceof PDType1Font);
      if (!unicode) {
        titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      }

      try (PDPageContentStream content = new PDPageContentStream(document, page)) {
        float y = 760;
        content.beginText();
        content.setLeading(18f);
        content.setFont(titleFont, 16);
        content.newLineAtOffset(50, y);
        content.showText("Training Services Contract");
        content.newLine();
        content.setFont(bodyFont, 12);
        content.showText("Contract number: " + toPdfText(contractNumber, unicode));
        content.newLine();
        content.showText("Generated at: " + generatedAt);
        content.newLine();
        content.newLine();
        content.showText("Student: " + toPdfText(buildFullName(person.getLastName(), person.getFirstName(), person.getMiddleName()), unicode));
        content.newLine();
        content.showText("Email: " + toPdfText(safe(person.getEmail()), unicode));
        content.newLine();
        content.showText("Phone: " + toPdfText(safe(person.getPhone()), unicode));
        content.newLine();
        content.newLine();
        content.showText("Course: " + toPdfText(courseName, unicode));
        content.newLine();
        content.showText("Semester: " + toPdfText(semesterName, unicode));
        content.newLine();
        content.showText("Group: " + toPdfText(safe(groupName), unicode));
        content.newLine();
        content.showText("Group teacher: " + toPdfText(safe(teacherName), unicode));
        content.newLine();
        content.newLine();
        content.showText("This document is generated automatically by the system.");
        content.endText();
      }

      document.save(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to generate contract PDF", e);
    }
  }

  private static String safe(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }

  private static String toPdfSafe(String value) {
    if (value == null) {
      return "-";
    }
    StringBuilder sb = new StringBuilder(value.length());
    for (char ch : value.toCharArray()) {
      if (ch >= 32 && ch <= 126) {
        sb.append(ch);
      } else {
        sb.append('?');
      }
    }
    return sb.toString();
  }

  private static String toPdfText(String value, boolean unicodeFontLoaded) {
    if (unicodeFontLoaded) {
      return safe(value);
    }
    return toPdfSafe(value);
  }

  private static PDFont resolveUnicodeFont(PDDocument document) {
    String[] candidates = {
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/dejavu/DejaVuSans.ttf",
        "C:/Windows/Fonts/arial.ttf"
    };
    for (String candidate : candidates) {
      File file = new File(candidate);
      if (file.exists()) {
        try {
          return PDType0Font.load(document, file);
        } catch (IOException ignored) {
          // try next candidate
        }
      }
    }
    return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  }

  private static String buildFullName(String lastName, String firstName, String middleName) {
    if (middleName == null || middleName.isBlank()) {
      return lastName + " " + firstName;
    }
    return lastName + " " + firstName + " " + middleName;
  }

  public record GeneratedContract(String contractNumber, byte[] content) {
  }
}
