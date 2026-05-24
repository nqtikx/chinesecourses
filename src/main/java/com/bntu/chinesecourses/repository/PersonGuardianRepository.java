package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.PersonGuardianEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonGuardianRepository extends JpaRepository<PersonGuardianEntity, Long> {

  Optional<PersonGuardianEntity> findByIdAndArchivedFalse(Long id);

  List<PersonGuardianEntity> findByChildPersonIdAndArchivedFalseOrderByPrimaryGuardianDescCreatedAtAsc(Long childPersonId);
}
