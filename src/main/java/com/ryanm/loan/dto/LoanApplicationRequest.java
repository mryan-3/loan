package com.ryanm.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.00", message = "Minimum loan amount is 100.00")
    private BigDecimal amount;

    @NotNull(message = "Term is required")
    @Min(value = 1, message = "Minimum term is 1 month")
    private Integer term;

    @NotBlank(message = "Purpose is required")
    private String purpose;
} 