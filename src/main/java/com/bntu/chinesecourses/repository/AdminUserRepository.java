package com.bntu.chinesecourses.repository;

import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUserEntity, UUID> {

  Optional<AdminUserEntity> findByUsername(String username);
}
