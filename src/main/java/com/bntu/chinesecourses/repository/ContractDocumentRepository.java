package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.ContractDocumentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractDocumentRepository extends JpaRepository<ContractDocumentEntity, Long> {
  List<ContractDocumentEntity> findTop200ByUserIdOrderByCreatedAtDesc(Long userId);

  List<ContractDocumentEntity> findTop500ByOrderByCreatedAtDesc();

  List<ContractDocumentEntity> findTop200ByGroupIdOrderByCreatedAtDesc(Long groupId);
}
