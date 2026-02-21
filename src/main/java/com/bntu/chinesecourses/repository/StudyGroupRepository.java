package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyGroupRepository extends JpaRepository<StudyGroupEntity, Long> {

  Optional<StudyGroupEntity> findByIdAndArchivedFalse(Long id);

  List<StudyGroupEntity> findTop50ByArchivedFalseAndSemester_IdOrderByNameAsc(Long semesterId);

  boolean existsByArchivedFalseAndSemester_IdAndNameIgnoreCase(Long semesterId, String name);

  boolean existsByArchivedFalseAndSemester_IdAndNameIgnoreCaseAndIdNot(
      Long semesterId,
      String name,
      Long id
  );
}
