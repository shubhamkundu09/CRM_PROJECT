package com.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadStatisticsUpdateDTO {
    private Boolean callsMade;
    private Boolean meetingBooked;
    private Boolean meetingDone;
}