package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.AdminRole;
import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUserEntity, Long> {

  Optional<AdminUserEntity> findByUsername(String username);
  boolean existsByGroupIdAndRole(Long groupId, AdminRole role);
}
