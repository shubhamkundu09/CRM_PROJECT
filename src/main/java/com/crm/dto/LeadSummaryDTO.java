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
public class LeadSummaryDTO {
    @JsonSerialize(using = EncryptSerializer.class)
    @JsonDeserialize(using = DecryptDeserializer.class)
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String leadType;
    private String leadStage;
    private LocalDate nextFollowUpDate;
    private String source;
    private LocalDateTime createdAt;
    private String assignedEmployeeName;
}