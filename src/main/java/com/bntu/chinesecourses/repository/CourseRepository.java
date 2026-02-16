package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.CourseEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<CourseEntity, Long> {

  List<CourseEntity> findTop50ByArchivedFalseOrderByCreatedAtDesc();
  boolean existsByArchivedFalseAndNameIgnoreCase(String name);
  boolean existsByArchivedFalseAndNameIgnoreCaseAndIdNot(String name, Long id);
}
