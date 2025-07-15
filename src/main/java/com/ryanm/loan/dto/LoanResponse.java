package com.ryanm.loan.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoanResponse {
    private Long id;
    private BigDecimal amount;
    private Integer term;
    private String purpose;
    private String status;
    private Long userId;
    private String userName;
    private Long reviewedById;
    private String reviewedByName;
    private String reviewComment;
    private String createdAt;
    private String updatedAt;
    private boolean userDeleted;
} 