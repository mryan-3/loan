package com.ryanm.loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoanReviewRequest {
    @NotNull(message = "Status is required")
    private String status; // Should be ACCEPTED or REJECTED

    @NotBlank(message = "Review comment is required")
    private String reviewComment;
} 