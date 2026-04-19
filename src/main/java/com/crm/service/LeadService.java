package com.crm.service;

import com.crm.dto.*;
import com.crm.entity.LeadStage;
import com.crm.entity.LeadType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LeadService {
    LeadResponseDTO createLead(LeadDTO leadDTO);
    LeadResponseDTO updateLead(Long id, LeadUpdateDTO leadUpdateDTO);
    void deleteLead(Long id);
    List<LeadResponseDTO> getLeadsByEmployee(Long employeeId);

    LeadResponseDTO getLeadById(Long id);

    Map<String, Long> getLeadStatistics();

    LeadResponseDTO updateLeadStatistics(Long id, LeadStatisticsUpdateDTO statisticsDTO);

    Page<LeadResponseDTO> searchLeads(
            String name,
            String email,
            String phone,
            LeadType leadType,
            LeadStage leadStage,
            Long assignedEmployeeId,
            Boolean isActive,
            String source,
            LocalDate nextFollowUpDate,
            LocalDate followUpFrom,
            LocalDate followUpTo,
            LocalDate createdFrom,
            LocalDate createdTo,
            LocalDate updatedFrom,
            LocalDate updatedTo,
            Integer minCallsMade,
            Integer maxCallsMade,
            Integer minMeetingsBooked,
            Integer maxMeetingsBooked,
            Integer minMeetingsDone,
            Integer maxMeetingsDone,
            Pageable pageable
    );

    LeadResponseDTO updateLeadStage(Long id, String stage, String employeeEmail);
    LeadResponseDTO updateFollowUp(Long id, String nextFollowUpDate, String nextFollowUpDescription, String employeeEmail);
    LeadResponseDTO updateLeadAfterContact(Long id, EmployeeLeadUpdateDTO updateDTO, String employeeEmail);
}