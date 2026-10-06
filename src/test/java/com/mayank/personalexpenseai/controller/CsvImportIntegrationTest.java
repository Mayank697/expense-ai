package com.mayank.personalexpenseai.controller;

import com.mayank.personalexpenseai.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.ai.ollama.base-url=http://localhost:11434",
    "spring.servlet.multipart.max-file-size=10MB",
    "spring.servlet.multipart.max-request-size=10MB"
})
@AutoConfigureMockMvc
class CsvImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseRepository expenseRepository;

    private static final String BASE_URL = "/api/expenses";

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
    }

    private MockMultipartFile createCsvFile(String content) {
        return new MockMultipartFile(
            "file",
            "expenses.csv",
            MediaType.TEXT_PLAIN_VALUE,
            content.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Nested
    @DisplayName("POST /api/expenses/import/csv - CSV Import")
    class CsvImport {

        @Test
        @DisplayName("Should import valid CSV and return success count")
        void shouldImportValidCsv() throws Exception {
            String csv = """
                date,amount,description,merchant,category,paymentMethod
                2025-10-01,99.99,Lunch at cafe,Cafe Nero,FOOD,CREDIT_CARD
                2025-10-02,50.00,Grocery shopping,Supermarket,GROCERIES,DEBIT_CARD
                2025-10-03,25.50,Bus pass,Transport Co,TRANSPORT,CASH
                """;

            MockMultipartFile file = createCsvFile(csv);

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.successCount").value(3))
                .andExpect(jsonPath("$.skipCount").value(0))
                .andExpect(jsonPath("$.errorCount").value(0))
                .andExpect(jsonPath("$.errors").isEmpty());

            // Verify expenses were saved
            mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
        }

        @Test
        @DisplayName("Should import CSV with dd/MM/yyyy date format")
        void shouldImportCsvWithDifferentDateFormat() throws Exception {
            String csv = """
                date,amount,description,merchant,category,paymentMethod
                01/10/2025,99.99,Lunch at cafe,Cafe Nero,FOOD,CREDIT_CARD
                02/10/2025,50.00,Grocery shopping,Supermarket,GROCERIES,DEBIT_CARD
                """;

            MockMultipartFile file = createCsvFile(csv);

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.errorCount").value(0));
        }

        @Test
        @DisplayName("Should import CSV with payment method aliases")
        void shouldImportCsvWithPaymentMethodAliases() throws Exception {
            String csv = """
                date,amount,description,merchant,category,paymentMethod
                2025-10-01,99.99,Lunch,Cafe Nero,FOOD,CC
                2025-10-02,50.00,Groceries,Supermarket,GROCERIES,DC
                2025-10-03,25.50,Transfer,Bank,UTILITIES,BANK
                """;

            MockMultipartFile file = createCsvFile(csv);

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.successCount").value(3))
                .andExpect(jsonPath("$.errorCount").value(0));
        }

        @Test
        @DisplayName("Should import CSV with dollar signs in amount")
        void shouldImportCsvWithDollarSigns() throws Exception {
            String csv = """
                date,amount,description,merchant,category,paymentMethod
                2025-10-01,$99.99,Lunch,Cafe Nero,FOOD,CREDIT_CARD
                2025-10-02,$50.00,Groceries,Supermarket,GROCERIES,DEBIT_CARD
                """;

            MockMultipartFile file = createCsvFile(csv);

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.errorCount").value(0));
        }

        @Test
        @DisplayName("Should handle CSV with empty rows")
        void shouldSkipEmptyRows() throws Exception {
            String csv = """
                date,amount,description,merchant,category,paymentMethod
                2025-10-01,99.99,Lunch,Cafe Nero,FOOD,CREDIT_CARD

                2025-10-02,50.00,Groceries,Supermarket,GROCERIES,DEBIT_CARD

                """;

            MockMultipartFile file = createCsvFile(csv);

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.skipCount").value(2));
        }

        @Test
        @DisplayName("Should handle CSV with some invalid rows")
        void shouldHandlePartialErrors() throws Exception {
            String csv = """
                date,amount,description,merchant,category,paymentMethod
                2025-10-01,99.99,Lunch,Cafe Nero,FOOD,CREDIT_CARD
                invalid-date,50.00,Groceries,Supermarket,GROCERIES,DEBIT_CARD
                2025-10-03,25.50,Bus,Transport,TRANSPORT,INVALID_METHOD
                """;

            MockMultipartFile file = createCsvFile(csv);

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.errorCount").value(2))
                .andExpect(jsonPath("$.errors.length()").value(2));
        }

        @Test
        @DisplayName("Should handle CSV with quoted fields containing commas")
        void shouldHandleQuotedFields() throws Exception {
            String csv = """
                date,amount,description,merchant,category,paymentMethod
                2025-10-01,99.99,"Lunch, dinner, and snacks","Cafe, Nero & Co",FOOD,CREDIT_CARD
                """;

            MockMultipartFile file = createCsvFile(csv);

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.errorCount").value(0));
        }

        @Test
        @DisplayName("Should return 400 for empty file")
        void shouldReturn400ForEmptyFile() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.csv",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
            );

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle CSV with missing columns")
        void shouldReportMissingColumns() throws Exception {
            String csv = """
                date,amount,description,merchant,category,paymentMethod
                2025-10-01,99.99,Lunch
                """;

            MockMultipartFile file = createCsvFile(csv);

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.successCount").value(0))
                .andExpect(jsonPath("$.errorCount").value(1));
        }

        @Test
        @DisplayName("Should handle CSV with unknown category")
        void shouldReportUnknownCategory() throws Exception {
            String csv = """
                date,amount,description,merchant,category,paymentMethod
                2025-10-01,99.99,Lunch,Cafe Nero,UNKNOWN_CAT,CREDIT_CARD
                """;

            MockMultipartFile file = createCsvFile(csv);

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.successCount").value(0))
                .andExpect(jsonPath("$.errorCount").value(1))
                .andExpect(jsonPath("$.errors[0].message", containsString("Unknown category")));
        }

        @Test
        @DisplayName("Should handle CSV with category aliases (spaces and dashes)")
        void shouldHandleCategoryAliases() throws Exception {
            String csv = """
                date,amount,description,merchant,category,paymentMethod
                2025-10-01,99.99,Lunch,Cafe Nero,Food,CREDIT_CARD
                2025-10-02,50.00,Groceries,Supermarket,Groceries,DEBIT_CARD
                2025-10-03,25.50,Bus,Transport,Transport,CASH
                """;

            MockMultipartFile file = createCsvFile(csv);

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.successCount").value(3))
                .andExpect(jsonPath("$.errorCount").value(0));
        }

        @Test
        @DisplayName("Should handle CSV with optional description and merchant as empty")
        void shouldHandleEmptyOptionalFields() throws Exception {
            String csv = """
                date,amount,description,merchant,category,paymentMethod
                2025-10-01,99.99,,,FOOD,CREDIT_CARD
                """;

            MockMultipartFile file = createCsvFile(csv);

            mockMvc.perform(multipart(BASE_URL + "/import/csv")
                    .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.errorCount").value(0));
        }
    }
}
