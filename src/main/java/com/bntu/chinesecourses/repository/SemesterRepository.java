package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.SemesterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepository extends JpaRepository<SemesterEntity, Long> {
}
