package com.bntu.chinesecourses.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      MethodArgumentNotValidException ex,
      HttpServletRequest request
  ) {
    List<ErrorResponse.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
        .map(this::toViolation)
        .toList();

    return build(HttpStatus.BAD_REQUEST, "Validation failed", request, violations);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler({UnauthorizedException.class, BadCredentialsException.class})
  public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException ex, HttpServletRequest request) {
    return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex, HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, "Access denied", request, List.of());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest request) {
    return build(HttpStatus.UNAUTHORIZED, "Unauthorized", request, List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, "Data integrity violation", request, List.of());
  }

  @ExceptionHandler(ErrorResponseException.class)
  public ResponseEntity<ErrorResponse> handleSpringWeb(ErrorResponseException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    String message = Optional.ofNullable(ex.getBody())
        .map(body -> body.getDetail())
        .orElse(ex.getMessage());

    return build(status, message, request, List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, List.of());
  }

  private ErrorResponse.FieldViolation toViolation(FieldError fieldError) {
    String message = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("Invalid value");
    return new ErrorResponse.FieldViolation(fieldError.getField(), message);
  }

  private ResponseEntity<ErrorResponse> build(
      HttpStatus status,
      String message,
      HttpServletRequest request,
      List<ErrorResponse.FieldViolation> violations
  ) {
    String traceId = Optional.ofNullable(request.getHeader("X-Trace-Id")).orElse(null);

    ErrorResponse body = new ErrorResponse(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        request.getRequestURI(),
        traceId,
        violations
    );

    return ResponseEntity.status(status).body(body);
  }
}
