package com.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedLoginResponse {
    private String token;
    private String type = "Bearer";
    private String email;
    private String role;  // "ADMIN" or "EMPLOYEE"
    private String message;
    private Boolean isFirstLogin;
}