package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.TeacherEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<TeacherEntity, Long> {
  Optional<TeacherEntity> findByIdAndArchivedFalse(Long id);
}
