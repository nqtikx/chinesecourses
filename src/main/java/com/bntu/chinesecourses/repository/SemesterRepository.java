package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.SemesterEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepository extends JpaRepository<SemesterEntity, Long> {
  List<SemesterEntity> findTop50ByArchivedFalseAndCourseIdOrderByStartDateDesc(Long courseId);
  boolean existsByArchivedFalseAndCourseIdAndNameIgnoreCase(Long courseId, String name);
  boolean existsByArchivedFalseAndCourseIdAndNameIgnoreCaseAndIdNot(Long courseId, String name, Long id);
}
