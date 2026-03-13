package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.AttendanceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

  Optional<AttendanceEntity> findByIdAndArchivedFalse(Long id);
  List<AttendanceEntity> findTop50ByArchivedFalseAndLessonSessionIdOrderByMarkedAtDesc(Long lessonSessionId);
  List<AttendanceEntity> findTop50ByArchivedFalseAndEnrollmentIdOrderByMarkedAtDesc(Long enrollmentId);
  List<AttendanceEntity> findByArchivedFalseAndLessonSessionIdInOrderByMarkedAtDesc(List<Long> lessonSessionIds);
}
