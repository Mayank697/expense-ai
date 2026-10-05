package com.mayank.personalexpenseai.dto;

import com.mayank.personalexpenseai.entity.ExpenseCategory;
import com.mayank.personalexpenseai.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount,

    @NotNull(message = "Expense date is required")
    LocalDate expenseDate,

    String description,

    String merchant,

    @NotNull(message = "Category is required")
    ExpenseCategory category,

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod
) {
}
