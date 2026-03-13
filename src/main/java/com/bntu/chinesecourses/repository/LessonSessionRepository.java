package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.LessonSessionEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonSessionRepository extends JpaRepository<LessonSessionEntity, Long> {

  Optional<LessonSessionEntity> findByIdAndArchivedFalse(Long id);
  List<LessonSessionEntity> findTop50ByArchivedFalseAndGroupIdOrderByStartsAtDesc(Long groupId);
  List<LessonSessionEntity> findTop50ByGroupIdOrderByStartsAtDesc(Long groupId);
  List<LessonSessionEntity> findByArchivedFalseAndGroupIdAndStartsAtBetweenOrderByStartsAtAsc(
      Long groupId,
      Instant fromInclusive,
      Instant toExclusive
  );
  List<LessonSessionEntity> findByArchivedFalseAndGroupIdAndIdInOrderByStartsAtAsc(Long groupId, List<Long> ids);
}
