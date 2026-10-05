package com.mayank.personalexpenseai.controller;

import com.mayank.personalexpenseai.dto.ExpenseRequest;
import com.mayank.personalexpenseai.entity.ExpenseCategory;
import com.mayank.personalexpenseai.entity.PaymentMethod;
import com.mayank.personalexpenseai.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.ai.ollama.base-url=http://localhost:11434"
})
@AutoConfigureMockMvc
class ExpenseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseRepository expenseRepository;

    private static final String BASE_URL = "/api/expenses";

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/expenses - Create Expense")
    class CreateExpense {

        @Test
        @DisplayName("Should create expense with valid data and return 201")
        void shouldCreateExpense() throws Exception {
            ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("99.99"),
                LocalDate.of(2025, 10, 1),
                "Lunch at cafe",
                "Cafe Nero",
                ExpenseCategory.FOOD,
                PaymentMethod.CREDIT_CARD
            );

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.amount").value("99.99"))
                .andExpect(jsonPath("$.expenseDate").value("2025-10-01"))
                .andExpect(jsonPath("$.description").value("Lunch at cafe"))
                .andExpect(jsonPath("$.merchant").value("Cafe Nero"))
                .andExpect(jsonPath("$.category").value("FOOD"))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        @DisplayName("Should create expense with null description and merchant")
        void shouldCreateExpenseWithNullOptionalFields() throws Exception {
            ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("50.00"),
                LocalDate.of(2025, 9, 15),
                null,
                null,
                ExpenseCategory.TRANSPORT,
                PaymentMethod.CASH
            );

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.merchant").doesNotExist());
        }

        @Test
        @DisplayName("Should return 400 when amount is missing")
        void shouldReturn400WhenAmountMissing() throws Exception {
            String json = """
                {
                    "expenseDate": "2025-10-01",
                    "category": "FOOD",
                    "paymentMethod": "CASH"
                }
                """;

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("amount"))
                .andExpect(jsonPath("$[0].message").value("Amount is required"));
        }

        @Test
        @DisplayName("Should return 400 when amount is zero")
        void shouldReturn400WhenAmountIsZero() throws Exception {
            String json = """
                {
                    "amount": 0,
                    "expenseDate": "2025-10-01",
                    "category": "FOOD",
                    "paymentMethod": "CASH"
                }
                """;

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("amount"));
        }

        @Test
        @DisplayName("Should return 400 when amount is negative")
        void shouldReturn400WhenAmountIsNegative() throws Exception {
            String json = """
                {
                    "amount": -10.00,
                    "expenseDate": "2025-10-01",
                    "category": "FOOD",
                    "paymentMethod": "CASH"
                }
                """;

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when expenseDate is missing")
        void shouldReturn400WhenDateMissing() throws Exception {
            String json = """
                {
                    "amount": 50.00,
                    "category": "FOOD",
                    "paymentMethod": "CASH"
                }
                """;

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("expenseDate"));
        }

        @Test
        @DisplayName("Should return 400 when category is missing")
        void shouldReturn400WhenCategoryMissing() throws Exception {
            String json = """
                {
                    "amount": 50.00,
                    "expenseDate": "2025-10-01",
                    "paymentMethod": "CASH"
                }
                """;

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("category"));
        }

        @Test
        @DisplayName("Should return 400 when paymentMethod is missing")
        void shouldReturn400WhenPaymentMethodMissing() throws Exception {
            String json = """
                {
                    "amount": 50.00,
                    "expenseDate": "2025-10-01",
                    "category": "FOOD"
                }
                """;

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("paymentMethod"));
        }

        @Test
        @DisplayName("Should return 400 when multiple fields are missing")
        void shouldReturn400WhenMultipleFieldsMissing() throws Exception {
            String json = """
                {
                    "description": "Only description"
                }
                """;

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", hasSize(greaterThan(1))));
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/{id} - Get Expense by ID")
    class GetExpenseById {

        @Test
        @DisplayName("Should return expense by ID")
        void shouldGetExpenseById() throws Exception {
            ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("75.50"),
                LocalDate.of(2025, 10, 5),
                "Grocery shopping",
                "Whole Foods",
                ExpenseCategory.GROCERIES,
                PaymentMethod.DEBIT_CARD
            );

            String createResponse = mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

            String id = objectMapper.readTree(createResponse).get("id").asText();

            mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.amount").value(75.5))
                .andExpect(jsonPath("$.category").value("GROCERIES"));
        }

        @Test
        @DisplayName("Should return 404 when expense not found")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");

            mockMvc.perform(get(BASE_URL + "/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$[0].message").value("Expense not found with id: " + nonExistentId));
        }
    }

    @Nested
    @DisplayName("GET /api/expenses - List All Expenses")
    class GetAllExpenses {

        @Test
        @DisplayName("Should return empty page when no expenses exist")
        void shouldReturnEmptyPage() throws Exception {
            mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should return all expenses with pagination")
        void shouldReturnAllExpenses() throws Exception {
            for (int i = 0; i < 3; i++) {
                ExpenseRequest request = new ExpenseRequest(
                    new BigDecimal("10." + i),
                    LocalDate.of(2025, 10, i + 1),
                    "Expense " + i,
                    "Merchant " + i,
                    ExpenseCategory.FOOD,
                    PaymentMethod.CASH
                );
                mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
            }

            mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));
        }

        @Test
        @DisplayName("Should respect page size parameter")
        void shouldRespectPageSize() throws Exception {
            for (int i = 0; i < 5; i++) {
                ExpenseRequest request = new ExpenseRequest(
                    new BigDecimal("10." + i),
                    LocalDate.of(2025, 10, i + 1),
                    "Expense " + i,
                    "Merchant " + i,
                    ExpenseCategory.FOOD,
                    PaymentMethod.CASH
                );
                mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
            }

            mockMvc.perform(get(BASE_URL).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("Should support page number parameter")
        void shouldSupportPagination() throws Exception {
            for (int i = 0; i < 5; i++) {
                ExpenseRequest request = new ExpenseRequest(
                    new BigDecimal("10." + i),
                    LocalDate.of(2025, 10, i + 1),
                    "Expense " + i,
                    "Merchant " + i,
                    ExpenseCategory.FOOD,
                    PaymentMethod.CASH
                );
                mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
            }

            mockMvc.perform(get(BASE_URL).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.number").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/category/{category} - Filter by Category")
    class GetByCategory {

        @Test
        @DisplayName("Should return expenses filtered by category")
        void shouldFilterByCategory() throws Exception {
            ExpenseRequest foodRequest = new ExpenseRequest(
                new BigDecimal("30.00"),
                LocalDate.of(2025, 10, 1),
                "Food expense",
                "Restaurant",
                ExpenseCategory.FOOD,
                PaymentMethod.CASH
            );
            ExpenseRequest transportRequest = new ExpenseRequest(
                new BigDecimal("15.00"),
                LocalDate.of(2025, 10, 2),
                "Transport expense",
                "Uber",
                ExpenseCategory.TRANSPORT,
                PaymentMethod.CREDIT_CARD
            );

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(foodRequest)))
                .andExpect(status().isCreated());

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transportRequest)))
                .andExpect(status().isCreated());

            mockMvc.perform(get(BASE_URL + "/category/FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].category").value("FOOD"));
        }

        @Test
        @DisplayName("Should return empty when no expenses match category")
        void shouldReturnEmptyForNoMatch() throws Exception {
            ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("30.00"),
                LocalDate.of(2025, 10, 1),
                "Food expense",
                "Restaurant",
                ExpenseCategory.FOOD,
                PaymentMethod.CASH
            );

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

            mockMvc.perform(get(BASE_URL + "/category/RENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/date-range - Filter by Date Range")
    class GetByDateRange {

        @Test
        @DisplayName("Should return expenses within date range")
        void shouldFilterByDateRange() throws Exception {
            ExpenseRequest octRequest = new ExpenseRequest(
                new BigDecimal("30.00"),
                LocalDate.of(2025, 10, 5),
                "October expense",
                "Merchant",
                ExpenseCategory.FOOD,
                PaymentMethod.CASH
            );
            ExpenseRequest novRequest = new ExpenseRequest(
                new BigDecimal("40.00"),
                LocalDate.of(2025, 11, 10),
                "November expense",
                "Merchant",
                ExpenseCategory.FOOD,
                PaymentMethod.CASH
            );

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(octRequest)))
                .andExpect(status().isCreated());

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(novRequest)))
                .andExpect(status().isCreated());

            mockMvc.perform(get(BASE_URL + "/date-range")
                    .param("startDate", "2025-10-01")
                    .param("endDate", "2025-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description").value("October expense"));
        }

        @Test
        @DisplayName("Should return empty when no expenses in range")
        void shouldReturnEmptyForNoMatch() throws Exception {
            ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("30.00"),
                LocalDate.of(2025, 10, 5),
                "October expense",
                "Merchant",
                ExpenseCategory.FOOD,
                PaymentMethod.CASH
            );

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

            mockMvc.perform(get(BASE_URL + "/date-range")
                    .param("startDate", "2025-11-01")
                    .param("endDate", "2025-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/merchant/{merchant} - Search by Merchant")
    class GetByMerchant {

        @Test
        @DisplayName("Should return expenses matching merchant (case insensitive)")
        void shouldSearchByMerchant() throws Exception {
            ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("50.00"),
                LocalDate.of(2025, 10, 1),
                "Shopping",
                "Amazon Store",
                ExpenseCategory.SHOPPING,
                PaymentMethod.CREDIT_CARD
            );

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

            mockMvc.perform(get(BASE_URL + "/merchant/amazon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].merchant").value("Amazon Store"));
        }

        @Test
        @DisplayName("Should return empty list when merchant not found")
        void shouldReturnEmptyWhenNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/merchant/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("PUT /api/expenses/{id} - Update Expense")
    class UpdateExpense {

        @Test
        @DisplayName("Should update expense and return updated data")
        void shouldUpdateExpense() throws Exception {
            ExpenseRequest createRequest = new ExpenseRequest(
                new BigDecimal("50.00"),
                LocalDate.of(2025, 10, 1),
                "Original description",
                "Original Merchant",
                ExpenseCategory.FOOD,
                PaymentMethod.CASH
            );

            String createResponse = mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

            String id = objectMapper.readTree(createResponse).get("id").asText();

            ExpenseRequest updateRequest = new ExpenseRequest(
                new BigDecimal("75.00"),
                LocalDate.of(2025, 10, 15),
                "Updated description",
                "Updated Merchant",
                ExpenseCategory.SHOPPING,
                PaymentMethod.CREDIT_CARD
            );

            mockMvc.perform(put(BASE_URL + "/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.amount").value(75.0))
                .andExpect(jsonPath("$.expenseDate").value("2025-10-15"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.merchant").value("Updated Merchant"))
                .andExpect(jsonPath("$.category").value("SHOPPING"))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent expense")
        void shouldReturn404WhenNotFound() throws Exception {
            ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("50.00"),
                LocalDate.of(2025, 10, 1),
                "Description",
                "Merchant",
                ExpenseCategory.FOOD,
                PaymentMethod.CASH
            );

            UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");

            mockMvc.perform(put(BASE_URL + "/" + nonExistentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$[0].message").value("Expense not found with id: " + nonExistentId));
        }

        @Test
        @DisplayName("Should return 400 when update request has invalid data")
        void shouldReturn400WhenInvalidData() throws Exception {
            ExpenseRequest createRequest = new ExpenseRequest(
                new BigDecimal("50.00"),
                LocalDate.of(2025, 10, 1),
                "Description",
                "Merchant",
                ExpenseCategory.FOOD,
                PaymentMethod.CASH
            );

            String createResponse = mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

            String id = objectMapper.readTree(createResponse).get("id").asText();

            String invalidJson = """
                {
                    "amount": -10,
                    "category": "FOOD",
                    "paymentMethod": "CASH"
                }
                """;

            mockMvc.perform(put(BASE_URL + "/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/expenses/{id} - Delete Expense")
    class DeleteExpense {

        @Test
        @DisplayName("Should delete expense and return 204")
        void shouldDeleteExpense() throws Exception {
            ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("50.00"),
                LocalDate.of(2025, 10, 1),
                "To be deleted",
                "Merchant",
                ExpenseCategory.FOOD,
                PaymentMethod.CASH
            );

            String createResponse = mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

            String id = objectMapper.readTree(createResponse).get("id").asText();

            mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNoContent());

            mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent expense")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");

            mockMvc.perform(delete(BASE_URL + "/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$[0].message").value("Expense not found with id: " + nonExistentId));
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/analytics/total - Total Spending")
    class TotalSpending {

        @Test
        @DisplayName("Should return total spending for date range")
        void shouldReturnTotalSpending() throws Exception {
            ExpenseRequest request1 = new ExpenseRequest(
                new BigDecimal("100.00"),
                LocalDate.of(2025, 10, 1),
                "Expense 1",
                "Merchant",
                ExpenseCategory.FOOD,
                PaymentMethod.CASH
            );
            ExpenseRequest request2 = new ExpenseRequest(
                new BigDecimal("200.00"),
                LocalDate.of(2025, 10, 15),
                "Expense 2",
                "Merchant",
                ExpenseCategory.SHOPPING,
                PaymentMethod.CREDIT_CARD
            );
            ExpenseRequest request3 = new ExpenseRequest(
                new BigDecimal("50.00"),
                LocalDate.of(2025, 11, 1),
                "November expense",
                "Merchant",
                ExpenseCategory.FOOD,
                PaymentMethod.CASH
            );

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());

            mockMvc.perform(get(BASE_URL + "/analytics/total")
                    .param("startDate", "2025-10-01")
                    .param("endDate", "2025-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(300.0));
        }

        @Test
        @DisplayName("Should return 0 when no expenses in range")
        void shouldReturnZeroWhenNoExpenses() throws Exception {
            mockMvc.perform(get(BASE_URL + "/analytics/total")
                    .param("startDate", "2025-10-01")
                    .param("endDate", "2025-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0.0));
        }
    }
}
