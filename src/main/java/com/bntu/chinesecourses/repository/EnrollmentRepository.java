package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.EnrollmentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {

  Optional<EnrollmentEntity> findByIdAndArchivedFalse(Long id);

  List<EnrollmentEntity> findTop50ByArchivedFalseAndSemesterIdOrderByIdAsc(Long semesterId);

  List<EnrollmentEntity> findTop200ByArchivedFalseAndGroupIdOrderByIdAsc(Long groupId);

  boolean existsByArchivedFalseAndStudentIdAndSemesterId(Long studentId, Long semesterId);
}
