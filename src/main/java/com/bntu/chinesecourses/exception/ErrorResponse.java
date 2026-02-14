package com.bntu.chinesecourses.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    String traceId,
    List<FieldViolation> violations
) {

  public record FieldViolation(
      String field,
      String message
  ) {
  }
}
