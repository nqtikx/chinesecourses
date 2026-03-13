package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.AcademicHolidayCreateRequest;
import com.bntu.chinesecourses.model.dto.AcademicHolidayResponse;
import com.bntu.chinesecourses.model.dto.AcademicHolidayUpdateRequest;
import com.bntu.chinesecourses.service.AcademicHolidayService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/holidays")
public class AcademicHolidayController {

  private final AcademicHolidayService academicHolidayService;

  public AcademicHolidayController(AcademicHolidayService academicHolidayService) {
    this.academicHolidayService = academicHolidayService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER','GROUP','USER')")
  public List<AcademicHolidayResponse> list() {
    return academicHolidayService.list();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public AcademicHolidayResponse create(@Valid @RequestBody AcademicHolidayCreateRequest request) {
    return academicHolidayService.create(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public AcademicHolidayResponse update(@PathVariable Long id, @Valid @RequestBody AcademicHolidayUpdateRequest request) {
    return academicHolidayService.update(id, request);
  }
}
