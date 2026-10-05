package com.mayank.personalexpenseai.service;

import com.mayank.personalexpenseai.dto.ExpenseRequest;
import com.mayank.personalexpenseai.dto.ExpenseResponse;
import com.mayank.personalexpenseai.entity.Expense;
import com.mayank.personalexpenseai.entity.ExpenseCategory;
import com.mayank.personalexpenseai.repository.ExpenseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public ExpenseResponse createExpense(ExpenseRequest request) {
        Expense expense = new Expense();
        expense.setAmount(request.amount());
        expense.setExpenseDate(request.expenseDate());
        expense.setDescription(request.description());
        expense.setMerchant(request.merchant());
        expense.setCategory(request.category());
        expense.setPaymentMethod(request.paymentMethod());

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(UUID id) {
        Expense expense = expenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Expense not found with id: " + id));
        return toResponse(expense);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getAllExpenses(Pageable pageable) {
        return expenseRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByCategory(ExpenseCategory category, Pageable pageable) {
        return expenseRepository.findByCategory(category, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return expenseRepository.findByExpenseDateBetween(startDate, endDate, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<Expense> getExpensesByMerchant(String merchant) {
        return expenseRepository.findByMerchantContainingIgnoreCase(merchant);
    }

    public ExpenseResponse updateExpense(UUID id, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Expense not found with id: " + id));

        expense.setAmount(request.amount());
        expense.setExpenseDate(request.expenseDate());
        expense.setDescription(request.description());
        expense.setMerchant(request.merchant());
        expense.setCategory(request.category());
        expense.setPaymentMethod(request.paymentMethod());

        Expense updated = expenseRepository.save(expense);
        return toResponse(updated);
    }

    public void deleteExpense(UUID id) {
        if (!expenseRepository.existsById(id)) {
            throw new RuntimeException("Expense not found with id: " + id);
        }
        expenseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getSpendingByCategory(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.sumByCategoryBetweenDates(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSpending(LocalDate startDate, LocalDate endDate) {
        BigDecimal total = expenseRepository.sumBetweenDates(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
            expense.getId(),
            expense.getAmount(),
            expense.getExpenseDate(),
            expense.getDescription(),
            expense.getMerchant(),
            expense.getCategory(),
            expense.getPaymentMethod(),
            expense.getCreatedAt(),
            expense.getUpdatedAt()
        );
    }
}
