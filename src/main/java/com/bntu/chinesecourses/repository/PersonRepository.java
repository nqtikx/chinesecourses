package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.PersonEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<PersonEntity, UUID> {

  List<PersonEntity> findTop50ByArchivedFalseAndLastNameStartingWithIgnoreCaseOrderByLastNameAscFirstNameAsc(String prefix);
}
