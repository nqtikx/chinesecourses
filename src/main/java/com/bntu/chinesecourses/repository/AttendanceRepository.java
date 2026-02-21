package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.AttendanceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

  List<AttendanceEntity> findTop200ByLessonSessionIdAndArchivedFalseOrderByIdAsc(Long lessonSessionId);
  Optional<AttendanceEntity> findByLessonSessionIdAndEnrollmentId(Long lessonSessionId, Long enrollmentId);
}
