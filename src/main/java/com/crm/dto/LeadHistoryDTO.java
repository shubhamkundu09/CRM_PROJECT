package com.crm.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class LeadHistoryDTO {
    private Long id;
    private String action;
    private String changes;
    private Map<String, FieldChangeDTO> detailedChanges;
    private String oldValues;
    private String newValues;
    private String remarks;
    private String previousStage;
    private String newStage;
    private LocalDateTime createdAt;
    private EmployeeBasicDTO employee;

    @Data
    public static class FieldChangeDTO {
        private String fieldName;
        private String oldValue;
        private String newValue;
    }
}