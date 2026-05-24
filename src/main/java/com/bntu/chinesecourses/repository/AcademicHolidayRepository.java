package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.AcademicHolidayEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicHolidayRepository extends JpaRepository<AcademicHolidayEntity, Long> {

  Optional<AcademicHolidayEntity> findByIdAndArchivedFalse(Long id);

  boolean existsByHolidayDateAndArchivedFalse(LocalDate holidayDate);

  List<AcademicHolidayEntity> findByArchivedFalseAndHolidayDateBetweenOrderByHolidayDateAsc(LocalDate from, LocalDate to);

  List<AcademicHolidayEntity> findTop200ByArchivedFalseOrderByHolidayDateAsc();
}
