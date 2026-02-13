package com.bntu.chinesecourses.security;

import com.bntu.chinesecourses.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

  private final JwtProperties jwtProperties;
  private final SecretKey secretKey;

  public JwtTokenService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(String subject, String role) {
    Instant now = Instant.now();
    Instant exp = now.plus(jwtProperties.accessTokenTtl());

    return Jwts.builder()
        .setIssuer(jwtProperties.issuer())
        .setSubject(subject)
        .claim("role", role)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parseClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }
}
