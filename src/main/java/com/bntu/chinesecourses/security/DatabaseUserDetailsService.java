package com.bntu.chinesecourses.security;

import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import com.bntu.chinesecourses.repository.AdminUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

  private final AdminUserRepository adminUserRepository;

  public DatabaseUserDetailsService(AdminUserRepository adminUserRepository) {
    this.adminUserRepository = adminUserRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    AdminUserEntity user =
        adminUserRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    return new SecurityPrincipal(
        user.getUsername(),
        user.getPasswordHash(),
        user.getRole(),
        user.getPersonId()
    );
  }
}
