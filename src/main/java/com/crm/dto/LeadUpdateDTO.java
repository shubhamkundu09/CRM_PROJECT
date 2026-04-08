package com.crm.dto;

import com.crm.entity.*;
import com.crm.util.DecryptDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadUpdateDTO {

    private String name;
    private String email;
    private String phoneNumber;
    private LeadType leadType;
    private LeadStage leadStage;

    @FutureOrPresent(message = "Next follow-up date must be today or in the future")
    private LocalDate nextFollowUpDate;

    private String remarks;
    private String nextFollowUp;

    @JsonDeserialize(using = DecryptDeserializer.class)
    private Long assignedEmployeeId;

    private String source;

    private MainService interestedService;
    private ServiceSubcategory serviceSubcategory;
    private ServiceSubSubcategory serviceSubSubcategory;
    private String serviceDescription;
}