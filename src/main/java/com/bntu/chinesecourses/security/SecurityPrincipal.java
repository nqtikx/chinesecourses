package com.bntu.chinesecourses.security;

import com.bntu.chinesecourses.model.entity.UserRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityPrincipal implements UserDetails {

  private final String username;
  private final String passwordHash;
  private final UserRole role;
  private final Long personId;

  public SecurityPrincipal(String username, String passwordHash, UserRole role, Long personId) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.role = role;
    this.personId = personId;
  }

  public UserRole getRole() {
    return role;
  }

  public Long getPersonId() {
    return personId;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
