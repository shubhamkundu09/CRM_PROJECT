package com.crm.dto;

import com.crm.entity.*;
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
public class LeadResponseDTO {
    @JsonSerialize(using = EncryptSerializer.class)
    @JsonDeserialize(using = DecryptDeserializer.class)
    private Long id;

    private String name;
    private String email;
    private String phoneNumber;
    private LeadType leadType;
    private LeadStage leadStage;
    private LocalDate nextFollowUpDate;
    private String remarks;
    private String nextFollowUp;
    private EmployeeResponseDTO assignedEmployee;
    private String source;
    private Boolean isActive;
    private Integer callsMadeCount;
    private Integer meetingsBookedCount;
    private Integer meetingsDoneCount;
    private Integer updateCount;
    private String lastUpdatedBy;
    private LocalDateTime lastContactDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private MainService interestedService;
    private ServiceSubcategory serviceSubcategory;
    private ServiceSubSubcategory serviceSubSubcategory;
    private String serviceDescription;
}