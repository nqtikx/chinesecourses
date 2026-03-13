package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.GroupScheduleTableResponse;
import com.bntu.chinesecourses.service.ScheduleTableService;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule-table")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER','USER','GROUP')")
public class ScheduleTableController {

  private final ScheduleTableService scheduleTableService;

  public ScheduleTableController(ScheduleTableService scheduleTableService) {
    this.scheduleTableService = scheduleTableService;
  }

  @GetMapping("/group/{groupId}")
  public GroupScheduleTableResponse table(
      @PathVariable Long groupId,
      @RequestParam(required = false) LocalDate weekStart
  ) {
    return scheduleTableService.getTable(groupId, weekStart);
  }

  @GetMapping("/group/{groupId}/export")
  public ResponseEntity<byte[]> export(
      @PathVariable Long groupId,
      @RequestParam(required = false) LocalDate weekStart
  ) {
    byte[] content = scheduleTableService.exportXlsx(groupId, weekStart);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.setContentDisposition(ContentDisposition.attachment()
        .filename("schedule-group-" + groupId + ".xlsx")
        .build());
    return ResponseEntity.ok().headers(headers).body(content);
  }
}
