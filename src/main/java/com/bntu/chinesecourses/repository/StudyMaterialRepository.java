package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.StudyMaterialEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterialEntity, Long> {
  List<StudyMaterialEntity> findTop200ByArchivedFalseAndGroup_IdOrderByCreatedAtDesc(Long groupId);

  Optional<StudyMaterialEntity> findByIdAndArchivedFalse(Long id);
}
