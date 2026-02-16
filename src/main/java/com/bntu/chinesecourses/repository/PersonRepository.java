package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.PersonEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<PersonEntity, Long> {
  List<PersonEntity> findTop50ByArchivedFalseOrderByLastNameAscFirstNameAsc();
  List<PersonEntity> findTop50ByArchivedFalseAndLastNameStartingWithIgnoreCaseOrderByLastNameAscFirstNameAsc(String prefix);
}
