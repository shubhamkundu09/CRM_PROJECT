package com.crm.dto;

import com.crm.entity.LeadStage;
import com.crm.entity.LeadType;
import com.crm.util.DecryptDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WebsiteLeadUpdateDTO {
    private LeadType leadType;
    private LeadStage leadStage;

    @JsonDeserialize(using = DecryptDeserializer.class)
    private Long assignedEmployeeId;

    private String remarks;
    private LocalDate nextFollowUpDate;
    private String nextFollowUp;
}