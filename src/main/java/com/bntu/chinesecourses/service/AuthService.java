package com.bntu.chinesecourses.service;

import com.bntu.chinesecourses.model.entity.AdminUserEntity;
import com.bntu.chinesecourses.security.JwtTokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final AdminUserService adminUserService;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService jwtTokenService;

  public AuthService(
      AdminUserService adminUserService,
      PasswordEncoder passwordEncoder,
      JwtTokenService jwtTokenService
  ) {
    this.adminUserService = adminUserService;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenService = jwtTokenService;
  }

  public String login(String username, String password) {
    AdminUserEntity user = adminUserService.findByUsername(username)
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid credentials");
    }

    return jwtTokenService.generateAccessToken(user.getUsername(), user.getRole().name());
  }
}
