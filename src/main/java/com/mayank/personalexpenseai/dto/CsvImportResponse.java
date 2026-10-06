package com.mayank.personalexpenseai.dto;

import java.util.List;

public record CsvImportResponse(
    int totalRows,
    int successCount,
    int skipCount,
    int errorCount,
    List<CsvImportError> errors
) {
}
