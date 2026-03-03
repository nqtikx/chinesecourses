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

  List<EnrollmentEntity> findTop50ByArchivedFalseAndGroupIdOrderByCreatedAtDesc(Long groupId);

  boolean existsByArchivedFalseAndStudentIdAndGroupIdIn(Long studentId, java.util.List<Long> groupIds);

  boolean existsByArchivedFalseAndStudentIdAndStatus(Long studentId, EnrollmentStatus status);

  java.util.Optional<EnrollmentEntity> findFirstByArchivedFalseAndStudentIdAndStatusOrderByCreatedAtDesc(
      Long studentId,
      EnrollmentStatus status
  );

  List<EnrollmentEntity> findByArchivedFalseAndStudentIdAndStatusOrderByCreatedAtDesc(Long studentId, EnrollmentStatus status);

  java.util.Optional<EnrollmentEntity> findFirstByArchivedFalseAndStudentIdAndGroupIdAndStatusOrderByCreatedAtDesc(
      Long studentId,
      Long groupId,
      EnrollmentStatus status
  );

  java.util.Optional<EnrollmentEntity> findFirstByArchivedFalseAndStudentIdAndSemesterIdAndStatusOrderByCreatedAtDesc(
      Long studentId,
      Long semesterId,
      EnrollmentStatus status
  );

  boolean existsByContractNumber(String contractNumber);
}
