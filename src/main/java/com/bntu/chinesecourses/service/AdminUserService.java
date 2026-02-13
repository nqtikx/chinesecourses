package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import java.util.Optional;

public interface AdminUserService {

  Optional<AdminUserEntity> findByUsername(String username);
}
