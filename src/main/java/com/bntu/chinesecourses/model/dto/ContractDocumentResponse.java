package com.bntu.chinesecourses.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ContractDocumentResponse(
    Long id,
    Long userId,
    Long courseId,
    Long groupId,
    String contractNumber,
    String fileName,
    BigDecimal basePrice,
    Integer discountPercent,
    BigDecimal finalPrice,
    Instant createdAt
) {
}
