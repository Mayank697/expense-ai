package com.mayank.personalexpenseai.dto;

import java.math.BigDecimal;

public record ErrorResponse(
    String field,
    String message
) {
}
