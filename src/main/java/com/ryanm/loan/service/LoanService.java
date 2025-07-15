package com.ryanm.loan.service;

import com.ryanm.loan.dto.LoanApplicationRequest;
import com.ryanm.loan.dto.LoanResponse;
import com.ryanm.loan.dto.LoanReviewRequest;
import com.ryanm.loan.dto.LoanUpdateRequest;
import com.ryanm.loan.exception.ResourceNotFoundException;
import com.ryanm.loan.exception.ValidationException;
import com.ryanm.loan.model.Loan;
import com.ryanm.loan.model.User;
import com.ryanm.loan.repository.LoanRepository;
import com.ryanm.loan.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public LoanResponse applyForLoan(String userEmail, LoanApplicationRequest request) {
        log.info("User {} applying for loan", userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        Loan loan = Loan.builder()
                .amount(request.getAmount())
                .term(request.getTerm())
                .purpose(request.getPurpose())
                .status(Loan.Status.PENDING)
                .user(user)
                .build();
        loanRepository.save(loan);
        log.info("Loan application submitted by user {}: loanId={}", userEmail, loan.getId());
        return mapToDto(loan);
    }

    public Page<LoanResponse> getAllLoans(int page, int size, String status, String sort, String direction) {
        Sort sortObj = Sort.by(sort != null ? sort : "createdAt");
        sortObj = "desc".equalsIgnoreCase(direction) ? sortObj.descending() : sortObj.ascending();
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Loan> loans = loanRepository.findAll(pageable);
        if (status != null) {
            List<Loan> filtered = loans.stream()
                .filter(loan -> loan.getStatus().name().equalsIgnoreCase(status))
                .toList();
            return new PageImpl<>(filtered.stream().map(this::mapToDto).toList(), pageable, filtered.size());
        } else {
            return loans.map(this::mapToDto);
        }
    }

    public LoanResponse getLoanById(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id.toString()));
        return mapToDto(loan);
    }

    @Transactional
    public LoanResponse approveLoan(Long loanId, String managerEmail, LoanReviewRequest request) {
        log.info("Manager {} approving loan {}", managerEmail, loanId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId.toString()));
        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", managerEmail));
        if (!"ACCEPTED".equalsIgnoreCase(request.getStatus())) {
            throw new ValidationException("Status must be ACCEPTED for approval");
        }
        loan.setStatus(Loan.Status.ACCEPTED);
        loan.setReviewedBy(manager);
        loan.setReviewComment(request.getReviewComment());
        loanRepository.save(loan);
        log.info("Loan {} approved by manager {}", loanId, managerEmail);
        return mapToDto(loan);
    }

    @Transactional
    public LoanResponse rejectLoan(Long loanId, String managerEmail, LoanReviewRequest request) {
        log.info("Manager {} rejecting loan {}", managerEmail, loanId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId.toString()));
        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", managerEmail));
        if (!"REJECTED".equalsIgnoreCase(request.getStatus())) {
            throw new ValidationException("Status must be REJECTED for rejection");
        }
        loan.setStatus(Loan.Status.REJECTED);
        loan.setReviewedBy(manager);
        loan.setReviewComment(request.getReviewComment());
        loanRepository.save(loan);
        log.info("Loan {} rejected by manager {}", loanId, managerEmail);
        return mapToDto(loan);
    }

    @Transactional
    public LoanResponse updateLoan(Long loanId, String userEmail, LoanUpdateRequest request) {
        log.info("User {} updating loan {}", userEmail, loanId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId.toString()));
        if (!loan.getUser().getEmail().equals(userEmail)) {
            throw new ValidationException("You can only update your own loans");
        }
        if (loan.getStatus() != Loan.Status.PENDING) {
            throw new ValidationException("Only pending loans can be updated");
        }
        if (request.getAmount() != null) loan.setAmount(request.getAmount());
        if (request.getTerm() != null) loan.setTerm(request.getTerm());
        if (request.getPurpose() != null) loan.setPurpose(request.getPurpose());
        loanRepository.save(loan);
        log.info("Loan {} updated by user {}", loanId, userEmail);
        return mapToDto(loan);
    }

    @Transactional
    public void deleteLoan(Long loanId, String userEmail) {
        log.info("User {} deleting loan {}", userEmail, loanId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId.toString()));
        if (!loan.getUser().getEmail().equals(userEmail)) {
            throw new ValidationException("You can only delete your own loans");
        }
        if (loan.getStatus() != Loan.Status.PENDING) {
            throw new ValidationException("Only pending loans can be deleted");
        }
        loanRepository.delete(loan);
        log.info("Loan {} deleted by user {}", loanId, userEmail);
    }

    public List<LoanResponse> getMyLoans(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        List<Loan> loans = loanRepository.findByUser(user);
        return loans.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Helper method to map Loan entity to LoanResponse DTO
    private LoanResponse mapToDto(Loan loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .amount(loan.getAmount())
                .term(loan.getTerm())
                .purpose(loan.getPurpose())
                .status(loan.getStatus().name())
                .userId(loan.getUser().getId())
                .userName(loan.getUser().getName())
                .reviewedById(loan.getReviewedBy() != null ? loan.getReviewedBy().getId() : null)
                .reviewedByName(loan.getReviewedBy() != null ? loan.getReviewedBy().getName() : null)
                .reviewComment(loan.getReviewComment())
                .createdAt(loan.getCreatedAt() != null ? loan.getCreatedAt().format(FORMATTER) : null)
                .updatedAt(loan.getUpdatedAt() != null ? loan.getUpdatedAt().format(FORMATTER) : null)
                .userDeleted(loan.getUser().getDeletedAt() != null)
                .build();
    }
} 