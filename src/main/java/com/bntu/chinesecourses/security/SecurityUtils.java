package com.bntu.chinesecourses.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

  private SecurityUtils() {}

  public static Optional<SecurityPrincipal> currentPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return Optional.empty();
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof SecurityPrincipal securityPrincipal) {
      return Optional.of(securityPrincipal);
    }
    return Optional.empty();
  }

  public static SecurityPrincipal requirePrincipal() {
    return currentPrincipal().orElseThrow(() -> new IllegalStateException("Unauthorized"));
  }
}
