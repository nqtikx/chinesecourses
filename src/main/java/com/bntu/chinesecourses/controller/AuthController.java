package com.bntu.chinesecourses.controller;

import com.bntu.chinesecourses.model.dto.AuthLoginRequest;
import com.bntu.chinesecourses.model.dto.AuthLoginResponse;
import com.bntu.chinesecourses.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
    String token = authService.login(request.username(), request.password());
    return ResponseEntity.ok(new AuthLoginResponse(token));
  }
}
