package com.crm.dto;

import com.crm.util.DecryptDeserializer;
import com.crm.util.EncryptSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDTO {
    @JsonSerialize(using = EncryptSerializer.class)
    @JsonDeserialize(using = DecryptDeserializer.class)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private String employeeCode;
    private String department;
    private String position;
    private Double salary;
    private String phoneNumber;
    private LocalDate joiningDate;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}