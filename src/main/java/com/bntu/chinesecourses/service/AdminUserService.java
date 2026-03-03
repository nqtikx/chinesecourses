package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import java.util.List;
import java.util.Optional;

public interface AdminUserService {

  Optional<AdminUserEntity> findByUsername(String username);

  Optional<AdminUserEntity> findById(Long id);

  List<AdminUserEntity> findAll();
}
