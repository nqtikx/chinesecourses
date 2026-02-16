package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyGroupRepository extends JpaRepository<StudyGroupEntity, Long> {
}
