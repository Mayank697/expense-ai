package com.mayank.personalexpenseai.service;

import com.mayank.personalexpenseai.dto.CsvImportError;
import com.mayank.personalexpenseai.dto.CsvImportResponse;
import com.mayank.personalexpenseai.entity.Expense;
import com.mayank.personalexpenseai.entity.ExpenseCategory;
import com.mayank.personalexpenseai.entity.PaymentMethod;
import com.mayank.personalexpenseai.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CsvImportService {

    private static final String[] EXPECTED_HEADERS = {"date", "amount", "description", "merchant", "category", "paymentMethod"};
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy")
    };

    private final ExpenseRepository expenseRepository;

    public CsvImportService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public CsvImportResponse importCsv(MultipartFile file) throws IOException {
        List<CsvImportError> errors = new ArrayList<>();
        List<Expense> expensesToSave = new ArrayList<>();
        int skipCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new CsvImportResponse(0, 0, 0, 1, List.of(new CsvImportError(0, "Empty CSV file")));
            }

            String[] headers = parseCsvLine(headerLine);
            validateHeaders(headers);

            int[] columnMapping = mapColumns(headers);
            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                String[] values = parseCsvLine(line);

                // Skip empty rows
                if (values.length == 1 && values[0].trim().isEmpty()) {
                    skipCount++;
                    continue;
                }

                if (values.length < 6) {
                    errors.add(new CsvImportError(rowNumber, "Expected 6 columns, found " + values.length));
                    continue;
                }

                try {
                    Expense expense = parseRow(values, columnMapping, rowNumber);
                    if (expense != null) {
                        expensesToSave.add(expense);
                    }
                } catch (Exception e) {
                    errors.add(new CsvImportError(rowNumber, e.getMessage()));
                }
            }

            // Bulk save
            if (!expensesToSave.isEmpty()) {
                expenseRepository.saveAll(expensesToSave);
            }

            int totalRows = rowNumber - 1; // Exclude header
            return new CsvImportResponse(
                totalRows,
                expensesToSave.size(),
                skipCount,
                errors.size(),
                errors
            );
        }
    }

    private void validateHeaders(String[] headers) {
        if (headers.length < 6) {
            throw new IllegalArgumentException("CSV must have at least 6 columns: " + String.join(", ", EXPECTED_HEADERS));
        }

        List<String> missingHeaders = new ArrayList<>();
        for (String expected : EXPECTED_HEADERS) {
            boolean found = false;
            for (String header : headers) {
                if (header.trim().equalsIgnoreCase(expected)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missingHeaders.add(expected);
            }
        }

        if (!missingHeaders.isEmpty()) {
            throw new IllegalArgumentException("Missing required columns: " + String.join(", ", missingHeaders));
        }
    }

    private int[] mapColumns(String[] headers) {
        int[] mapping = new int[6];
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            for (int j = 0; j < headers.length; j++) {
                if (headers[j].trim().equalsIgnoreCase(EXPECTED_HEADERS[i])) {
                    mapping[i] = j;
                    break;
                }
            }
        }
        return mapping;
    }

    private Expense parseRow(String[] values, int[] columnMapping, int rowNumber) {
        Expense expense = new Expense();

        // Parse date
        String dateStr = values[columnMapping[0]].trim();
        expense.setExpenseDate(parseDate(dateStr, rowNumber));

        // Parse amount
        String amountStr = values[columnMapping[1]].trim().replaceAll("[,$]", "");
        expense.setAmount(new BigDecimal(amountStr));

        // Parse description (optional)
        String description = values[columnMapping[2]].trim();
        if (!description.isEmpty()) {
            expense.setDescription(description);
        }

        // Parse merchant (optional)
        String merchant = values[columnMapping[3]].trim();
        if (!merchant.isEmpty()) {
            expense.setMerchant(merchant);
        }

        // Parse category
        String categoryStr = values[columnMapping[4]].trim().toUpperCase().replaceAll("[\\s-]+", "_");
        expense.setCategory(parseCategory(categoryStr, rowNumber));

        // Parse payment method
        String paymentMethodStr = values[columnMapping[5]].trim().toUpperCase().replaceAll("[\\s-]+", "_");
        expense.setPaymentMethod(parsePaymentMethod(paymentMethodStr, rowNumber));

        return expense;
    }

    private LocalDate parseDate(String dateStr, int rowNumber) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        throw new IllegalArgumentException("Row " + rowNumber + ": Invalid date format: " + dateStr + ". Supported formats: yyyy-MM-dd, dd/MM/yyyy, MM/dd/yyyy");
    }

    private ExpenseCategory parseCategory(String categoryStr, int rowNumber) {
        try {
            return ExpenseCategory.valueOf(categoryStr);
        } catch (IllegalArgumentException e) {
            // Try to find a close match
            for (ExpenseCategory category : ExpenseCategory.values()) {
                if (category.name().startsWith(categoryStr) || categoryStr.startsWith(category.name())) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Row " + rowNumber + ": Unknown category: " + categoryStr + ". Valid categories: " +
                java.util.Arrays.stream(ExpenseCategory.values()).map(Enum::name).collect(Collectors.joining(", ")));
        }
    }

    private PaymentMethod parsePaymentMethod(String paymentMethodStr, int rowNumber) {
        try {
            return PaymentMethod.valueOf(paymentMethodStr);
        } catch (IllegalArgumentException e) {
            // Try common aliases
            if (paymentMethodStr.equals("CC") || paymentMethodStr.equals("CREDIT")) {
                return PaymentMethod.CREDIT_CARD;
            }
            if (paymentMethodStr.equals("DC") || paymentMethodStr.equals("DEBIT")) {
                return PaymentMethod.DEBIT_CARD;
            }
            if (paymentMethodStr.equals("BANK") || paymentMethodStr.equals("TRANSFER")) {
                return PaymentMethod.BANK_TRANSFER;
            }

            throw new IllegalArgumentException("Row " + rowNumber + ": Unknown payment method: " + paymentMethodStr + ". Valid methods: " +
                java.util.Arrays.stream(PaymentMethod.values()).map(Enum::name).collect(Collectors.joining(", ")));
        }
    }

    /**
     * Parse a CSV line handling quoted fields with commas inside.
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
