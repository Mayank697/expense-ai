package com.mayank.personalexpenseai.controller;

import com.mayank.personalexpenseai.dto.ExpenseRequest;
import com.mayank.personalexpenseai.dto.ExpenseResponse;
import com.mayank.personalexpenseai.entity.Expense;
import com.mayank.personalexpenseai.entity.ExpenseCategory;
import com.mayank.personalexpenseai.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(@Valid @RequestBody ExpenseRequest request) {
        return expenseService.createExpense(request);
    }

    @GetMapping("/{id}")
    public ExpenseResponse getExpense(@PathVariable UUID id) {
        return expenseService.getExpenseById(id);
    }

    @GetMapping
    public Page<ExpenseResponse> getAllExpenses(@PageableDefault(size = 20) Pageable pageable) {
        return expenseService.getAllExpenses(pageable);
    }

    @GetMapping("/category/{category}")
    public Page<ExpenseResponse> getExpensesByCategory(
        @PathVariable ExpenseCategory category,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return expenseService.getExpensesByCategory(category, pageable);
    }

    @GetMapping("/date-range")
    public Page<ExpenseResponse> getExpensesByDateRange(
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return expenseService.getExpensesByDateRange(startDate, endDate, pageable);
    }

    @GetMapping("/merchant/{merchant}")
    public List<Expense> getExpensesByMerchant(@PathVariable String merchant) {
        return expenseService.getExpensesByMerchant(merchant);
    }

    @PutMapping("/{id}")
    public ExpenseResponse updateExpense(@PathVariable UUID id, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.updateExpense(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable UUID id) {
        expenseService.deleteExpense(id);
    }

    @GetMapping("/analytics/total")
    public BigDecimal getTotalSpending(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return expenseService.getTotalSpending(startDate, endDate);
    }
}
