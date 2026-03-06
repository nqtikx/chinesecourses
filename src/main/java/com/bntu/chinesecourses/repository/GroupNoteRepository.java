package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.GroupNoteEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupNoteRepository extends JpaRepository<GroupNoteEntity, Long> {
  List<GroupNoteEntity> findTop200ByArchivedFalseAndGroup_IdOrderByCreatedAtDesc(Long groupId);
}
