package com.ryanm.loan.repository;

import com.ryanm.loan.model.Loan;
import com.ryanm.loan.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUser(User user);
    // Add more query methods as needed for filtering
} 