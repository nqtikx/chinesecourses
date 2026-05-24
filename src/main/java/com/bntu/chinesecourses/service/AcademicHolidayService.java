package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.exception.ConflictException;
import com.bntu.chinesecourses.exception.NotFoundException;
import com.bntu.chinesecourses.model.dto.AcademicHolidayCreateRequest;
import com.bntu.chinesecourses.model.dto.AcademicHolidayResponse;
import com.bntu.chinesecourses.model.dto.AcademicHolidayUpdateRequest;
import com.bntu.chinesecourses.model.entity.AcademicHolidayEntity;
import com.bntu.chinesecourses.repository.AcademicHolidayRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicHolidayService {

  private final AcademicHolidayRepository academicHolidayRepository;

  public AcademicHolidayService(AcademicHolidayRepository academicHolidayRepository) {
    this.academicHolidayRepository = academicHolidayRepository;
  }

  @Transactional(readOnly = true)
  public List<AcademicHolidayResponse> list() {
    return academicHolidayRepository.findTop200ByArchivedFalseOrderByHolidayDateAsc()
        .stream()
        .map(AcademicHolidayService::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AcademicHolidayEntity> listForRange(LocalDate from, LocalDate to) {
    return academicHolidayRepository.findByArchivedFalseAndHolidayDateBetweenOrderByHolidayDateAsc(from, to);
  }

  @Transactional
  public AcademicHolidayResponse create(AcademicHolidayCreateRequest request) {
    if (academicHolidayRepository.existsByHolidayDateAndArchivedFalse(request.holidayDate())) {
      throw new ConflictException("Holiday already exists for date " + request.holidayDate());
    }
    AcademicHolidayEntity entity = new AcademicHolidayEntity(
        null,
        request.holidayDate(),
        request.title().trim(),
        request.noClasses(),
        false,
        Instant.now());
    return toResponse(academicHolidayRepository.save(entity));
  }

  @Transactional
  public AcademicHolidayResponse update(Long id, AcademicHolidayUpdateRequest request) {
    AcademicHolidayEntity entity = academicHolidayRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Holiday not found id=" + id));
    if (!entity.getHolidayDate().equals(request.holidayDate())
        && academicHolidayRepository.existsByHolidayDateAndArchivedFalse(request.holidayDate())) {
      throw new ConflictException("Holiday already exists for date " + request.holidayDate());
    }
    entity.setHolidayDate(request.holidayDate());
    entity.setTitle(request.title().trim());
    entity.setNoClasses(request.noClasses());
    entity.setArchived(request.archived());
    return toResponse(entity);
  }

  private static AcademicHolidayResponse toResponse(AcademicHolidayEntity entity) {
    return new AcademicHolidayResponse(
        entity.getId(),
        entity.getHolidayDate(),
        entity.getTitle(),
        entity.isNoClasses(),
        entity.isArchived(),
        entity.getCreatedAt());
  }
}
