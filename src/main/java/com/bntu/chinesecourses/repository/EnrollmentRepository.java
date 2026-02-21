package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.ChineseLevel;
import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import com.bntu.chinesecourses.model.entity.EnrollmentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {

  boolean existsByArchivedFalseAndStudentIdAndSemesterId(Long studentId, Long semesterId);

  List<EnrollmentEntity> findTop50ByArchivedFalseAndSemesterIdAndStatusAndLevelOrderByCreatedAtDesc(
      Long semesterId,
      EnrollmentStatus status,
      ChineseLevel level
  );
}
