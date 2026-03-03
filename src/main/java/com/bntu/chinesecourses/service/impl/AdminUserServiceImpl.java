package com.bntu.chinesecourses.service.impl;

import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import com.bntu.chinesecourses.repository.AdminUserRepository;
import com.bntu.chinesecourses.service.AdminUserService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

  private final AdminUserRepository adminUserRepository;

  public AdminUserServiceImpl(AdminUserRepository adminUserRepository) {
    this.adminUserRepository = adminUserRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AdminUserEntity> findByUsername(String username) {
    return adminUserRepository.findByUsername(username);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AdminUserEntity> findById(Long id) {
    return adminUserRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminUserEntity> findAll() {
    return adminUserRepository.findAll();
  }
}
