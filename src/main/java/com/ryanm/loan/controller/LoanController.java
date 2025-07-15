package com.ryanm.loan.controller;

import com.ryanm.loan.dto.LoanApplicationRequest;
import com.ryanm.loan.dto.LoanResponse;
import com.ryanm.loan.dto.LoanReviewRequest;
import com.ryanm.loan.dto.LoanUpdateRequest;
import com.ryanm.loan.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Slf4j
public class LoanController {
    private final LoanService loanService;

    // 1. Apply for a loan (customer)
    @PostMapping("/apply")
    public ResponseEntity<LoanResponse> applyForLoan(@AuthenticationPrincipal UserDetails userDetails,
                                                     @Valid @RequestBody LoanApplicationRequest request) {
        log.info("API: Apply for loan by user: {}", userDetails.getUsername());
        LoanResponse response = loanService.applyForLoan(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    // 2. List all loans (manager, auditor)
    @GetMapping("")
    public ResponseEntity<Page<LoanResponse>> getAllLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        log.info("API: Get all loans");
        Page<LoanResponse> response = loanService.getAllLoans(page, size, status, sort, direction);
        return ResponseEntity.ok(response);
    }

    // 3. Get a specific loan
    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> getLoanById(@PathVariable Long id) {
        log.info("API: Get loan by id: {}", id);
        LoanResponse response = loanService.getLoanById(id);
        return ResponseEntity.ok(response);
    }

    // 4. Approve a loan (manager)
    @PostMapping("/{id}/approve")
    public ResponseEntity<LoanResponse> approveLoan(@AuthenticationPrincipal UserDetails userDetails,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody LoanReviewRequest request) {
        log.info("API: Approve loan {} by manager: {}", id, userDetails.getUsername());
        LoanResponse response = loanService.approveLoan(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    // 5. Reject a loan (manager)
    @PostMapping("/{id}/reject")
    public ResponseEntity<LoanResponse> rejectLoan(@AuthenticationPrincipal UserDetails userDetails,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody LoanReviewRequest request) {
        log.info("API: Reject loan {} by manager: {}", id, userDetails.getUsername());
        LoanResponse response = loanService.rejectLoan(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    // 6. Update loan details (customer)
    @PatchMapping("/{id}")
    public ResponseEntity<LoanResponse> updateLoan(@AuthenticationPrincipal UserDetails userDetails,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody LoanUpdateRequest request) {
        log.info("API: Update loan {} by user: {}", id, userDetails.getUsername());
        LoanResponse response = loanService.updateLoan(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    // 7. Delete loan application (customer)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Long id) {
        log.info("API: Delete loan {} by user: {}", id, userDetails.getUsername());
        loanService.deleteLoan(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // 8. List current user's loans (customer)
    @GetMapping("/my")
    public ResponseEntity<List<LoanResponse>> getMyLoans(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("API: Get my loans for user: {}", userDetails.getUsername());
        List<LoanResponse> response = loanService.getMyLoans(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
} 