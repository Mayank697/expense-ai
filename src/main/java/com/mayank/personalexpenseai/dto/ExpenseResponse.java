package com.mayank.personalexpenseai.dto;

import com.mayank.personalexpenseai.entity.ExpenseCategory;
import com.mayank.personalexpenseai.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseResponse(
    UUID id,
    BigDecimal amount,
    LocalDate expenseDate,
    String description,
    String merchant,
    ExpenseCategory category,
    PaymentMethod paymentMethod,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
