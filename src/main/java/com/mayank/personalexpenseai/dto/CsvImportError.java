package com.mayank.personalexpenseai.dto;

public record CsvImportError(
    int rowNumber,
    String message
) {
}
