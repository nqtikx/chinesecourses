package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.StudyGroupEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyGroupRepository extends JpaRepository<StudyGroupEntity, Long> {


  @Query(
      value = """
        SELECT sg.*
        FROM study_group sg
        JOIN enrollment e ON e.group_id = sg.id AND e.archived = false
        JOIN person p ON p.id = e.student_id AND p.archived = false
        WHERE sg.archived = false
          AND (
            LOWER(p.last_name) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.first_name) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(COALESCE(p.middle_name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(CONCAT(p.last_name, ' ', p.first_name, ' ', COALESCE(p.middle_name, ''))) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY sg.name ASC
        LIMIT 50
        """,
      nativeQuery = true
  )
  List<StudyGroupEntity> searchTop50ByStudentFio(@Param("q") String query);

  Optional<StudyGroupEntity> findByIdAndArchivedFalse(Long id);

  List<StudyGroupEntity> findTop50ByArchivedFalseAndSemester_IdOrderByNameAsc(Long semesterId);

  boolean existsByArchivedFalseAndSemester_IdAndNameIgnoreCase(Long semesterId, String name);

  List<StudyGroupEntity> findTop50ByArchivedFalseAndTeacher_IdOrderByNameAsc(Long teacherId);

  boolean existsByArchivedFalseAndSemester_IdAndNameIgnoreCaseAndIdNot(
      Long semesterId,
      String name,
      Long id
  );
}
