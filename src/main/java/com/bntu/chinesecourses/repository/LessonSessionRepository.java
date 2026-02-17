package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.LessonSessionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonSessionRepository extends JpaRepository<LessonSessionEntity, Long> {

  Optional<LessonSessionEntity> findByIdAndArchivedFalse(Long id);
  List<LessonSessionEntity> findTop50ByArchivedFalseAndGroupIdOrderByStartsAtDesc(Long groupId);
}
