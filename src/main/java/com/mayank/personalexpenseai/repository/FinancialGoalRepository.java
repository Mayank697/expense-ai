package com.mayank.personalexpenseai.repository;

import com.mayank.personalexpenseai.entity.FinancialGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, UUID> {
}
