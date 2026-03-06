package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.GroupScheduleTableResponse;
import com.bntu.chinesecourses.model.dto.ScheduleCellResponse;
import com.bntu.chinesecourses.model.entity.CourseEntity;
import com.bntu.chinesecourses.model.entity.GroupScheduleRuleEntity;
import com.bntu.chinesecourses.model.entity.SemesterEntity;
import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import com.bntu.chinesecourses.repository.CourseRepository;
import com.bntu.chinesecourses.repository.GroupScheduleRuleRepository;
import com.bntu.chinesecourses.repository.SemesterRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleTableService {
  private static final String[] DAY_LABELS = {"", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

  private final GroupAccessService groupAccessService;
  private final GroupScheduleRuleRepository groupScheduleRuleRepository;
  private final SemesterRepository semesterRepository;
  private final CourseRepository courseRepository;

  public ScheduleTableService(
      GroupAccessService groupAccessService,
      GroupScheduleRuleRepository groupScheduleRuleRepository,
      SemesterRepository semesterRepository,
      CourseRepository courseRepository
  ) {
    this.groupAccessService = groupAccessService;
    this.groupScheduleRuleRepository = groupScheduleRuleRepository;
    this.semesterRepository = semesterRepository;
    this.courseRepository = courseRepository;
  }

  @Transactional(readOnly = true)
  public GroupScheduleTableResponse getTable(Long groupId) {
    StudyGroupEntity group = groupAccessService.requireVisibleGroup(groupId);
    Long semesterId = Objects.requireNonNull(group.getSemesterId(), "Group semesterId is null");
    SemesterEntity semester = semesterRepository.findById(semesterId)
        .orElseThrow(() -> new NotFoundException("Semester not found id=" + semesterId));
    Long courseId = Objects.requireNonNull(semester.getCourseId(), "Semester courseId is null");
    CourseEntity course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException("Course not found id=" + courseId));

    String teacherName = "-";
    if (group.getTeacher() != null && group.getTeacher().getPerson() != null) {
      var p = group.getTeacher().getPerson();
      teacherName = p.getLastName() + " " + p.getFirstName();
    }

    List<ScheduleCellResponse> rows = groupScheduleRuleRepository
        .findTop50ByArchivedFalseAndGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId)
        .stream()
        .map(this::toRow)
        .toList();

    return new GroupScheduleTableResponse(
        group.getId(),
        group.getName(),
        course.getName(),
        semester.getName(),
        teacherName,
        rows
    );
  }

  @Transactional(readOnly = true)
  public byte[] exportXlsx(Long groupId) {
    GroupScheduleTableResponse table = getTable(groupId);
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      XSSFSheet sheet = workbook.createSheet("Schedule");

      XSSFFont titleFont = workbook.createFont();
      titleFont.setBold(true);
      titleFont.setFontHeightInPoints((short) 14);

      XSSFFont headerFont = workbook.createFont();
      headerFont.setBold(true);

      CellStyle titleStyle = workbook.createCellStyle();
      titleStyle.setFont(titleFont);
      titleStyle.setAlignment(HorizontalAlignment.CENTER);
      titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

      CellStyle metaKeyStyle = workbook.createCellStyle();
      metaKeyStyle.setFont(headerFont);
      metaKeyStyle.setAlignment(HorizontalAlignment.RIGHT);

      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      setAllBorders(headerStyle);
      headerStyle.setAlignment(HorizontalAlignment.CENTER);

      CellStyle cellStyle = workbook.createCellStyle();
      setAllBorders(cellStyle);
      cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

      CellStyle timeStyle = workbook.createCellStyle();
      setAllBorders(timeStyle);
      timeStyle.setAlignment(HorizontalAlignment.CENTER);

      int rowNum = 0;
      Row title = sheet.createRow(rowNum++);
      title.setHeightInPoints(26f);
      Cell titleCell = title.createCell(0);
      titleCell.setCellValue("Расписание учебной группы");
      titleCell.setCellStyle(titleStyle);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

      rowNum = writeMeta(sheet, rowNum, "Группа:", table.groupName(), metaKeyStyle);
      rowNum = writeMeta(sheet, rowNum, "Курс:", table.courseName(), metaKeyStyle);
      rowNum = writeMeta(sheet, rowNum, "Семестр:", table.semesterName(), metaKeyStyle);
      rowNum = writeMeta(sheet, rowNum, "Преподаватель:", table.teacherName(), metaKeyStyle);
      rowNum++;

      Row header = sheet.createRow(rowNum++);
      String[] headers = {"День", "Начало", "Окончание", "Аудитория/ссылка", "Тип занятия"};
      for (int i = 0; i < headers.length; i++) {
        Cell c = header.createCell(i);
        c.setCellValue(headers[i]);
        c.setCellStyle(headerStyle);
      }

      for (ScheduleCellResponse r : table.rows()) {
        Row row = sheet.createRow(rowNum++);
        Cell c0 = row.createCell(0);
        c0.setCellValue(r.dayLabel());
        c0.setCellStyle(cellStyle);

        Cell c1 = row.createCell(1);
        c1.setCellValue(r.startTime());
        c1.setCellStyle(timeStyle);

        Cell c2 = row.createCell(2);
        c2.setCellValue(r.endTime());
        c2.setCellStyle(timeStyle);

        Cell c3 = row.createCell(3);
        c3.setCellValue(r.room());
        c3.setCellStyle(cellStyle);

        Cell c4 = row.createCell(4);
        c4.setCellValue("Практика");
        c4.setCellStyle(cellStyle);
      }

      for (int i = 0; i < 5; i++) {
        sheet.autoSizeColumn(i);
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to export schedule xlsx", e);
    }
  }

  private ScheduleCellResponse toRow(GroupScheduleRuleEntity e) {
    int day = e.getDayOfWeek();
    String dayLabel = day >= 1 && day <= 7 ? DAY_LABELS[day] : String.valueOf(day);
    return new ScheduleCellResponse(
        day,
        dayLabel,
        e.getStartTime() == null ? "" : e.getStartTime().toString(),
        e.getEndTime() == null ? "" : e.getEndTime().toString(),
        e.getRoom() == null ? "" : e.getRoom()
    );
  }

  private static int writeMeta(XSSFSheet sheet, int rowNum, String key, String value, CellStyle keyStyle) {
    Row row = sheet.createRow(rowNum);
    Cell k = row.createCell(0);
    k.setCellValue(key);
    k.setCellStyle(keyStyle);
    row.createCell(1).setCellValue(value);
    return rowNum + 1;
  }

  private static void setAllBorders(CellStyle style) {
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
  }
}
