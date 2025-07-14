package com.ryanm.loan.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String image;
    private String phone;
    private String role;
    private String createdAt;
    private String updatedAt;
} 