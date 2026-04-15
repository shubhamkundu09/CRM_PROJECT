package com.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private Boolean isActive;
    private Integer totalLeads;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}