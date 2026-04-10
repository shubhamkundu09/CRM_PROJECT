package com.crm.service;

import com.crm.dto.EmployeeLeadUpdateDTO;
import com.crm.dto.EmployeeResponseDTO;
import com.crm.dto.LeadResponseDTO;
import com.crm.entity.Employee;
import com.crm.entity.Lead;
import com.crm.entity.LeadStage;
import com.crm.exception.ResourceNotFoundException;
import com.crm.exception.UnauthorizedException;
import com.crm.repository.EmployeeRepository;
import com.crm.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeLeadServiceImpl implements EmployeeLeadService {

    private final LeadRepository leadRepository;
    private final EmployeeRepository employeeRepository;
    private final LeadHistoryService leadHistoryService;
    private final EmailService emailService;

    @Override
    public LeadResponseDTO updateLead(Long leadId, EmployeeLeadUpdateDTO updateDTO, String employeeEmail) {
        log.info("Employee {} performing consolidated update for lead {}", employeeEmail, leadId);

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + leadId));

        // Verify employee has access to this lead
        if (!lead.getAssignedEmployee().getId().equals(employee.getId())) {
            throw new UnauthorizedException("You are not authorized to update this lead");
        }

        // Create a copy of lead before changes for comparison
        Lead oldLead = copyLead(lead);
        List<String> changes = new ArrayList<>();
        String oldStage = lead.getLeadStage().toString();

        // 1. Handle contact made
        if (Boolean.TRUE.equals(updateDTO.getContactMade())) {
            String contactInfo = processContactMade(lead, updateDTO);
            changes.add(contactInfo);
            lead.setLastContactDate(LocalDateTime.now());

            // Record contact made in history
            leadHistoryService.recordContactMade(lead, employee, "Phone/Email",
                    updateDTO.getResponseMessage() != null ? updateDTO.getResponseMessage() : "Contact attempted",
                    updateDTO.getRemarks());
        }

        // 2. Handle statistics updates
        List<String> statsChanges = processStatisticsUpdates(lead, updateDTO);
        changes.addAll(statsChanges);

        // 3. Handle stage update
        if (updateDTO.getNewLeadStage() != null && updateDTO.getNewLeadStage() != lead.getLeadStage()) {
            if (isValidLeadStage(updateDTO.getNewLeadStage())) {
                changes.add("Stage changed from " + oldStage + " to " + updateDTO.getNewLeadStage());
                leadHistoryService.recordStageChange(lead, employee, oldStage, updateDTO.getNewLeadStage().toString(),
                        "Stage updated by employee: " + employee.getEmail());
                lead.setLeadStage(updateDTO.getNewLeadStage());
            } else {
                throw new IllegalArgumentException("Invalid lead stage. Allowed stages: INTERESTED, NOT_INTERESTED, NORMAL");
            }
        }

        // 4. Handle follow-up update
        String oldFollowUpDate = lead.getNextFollowUpDate().toString();
        String oldFollowUpDesc = lead.getNextFollowUp();

        if (updateDTO.getNextFollowUpDate() != null ||
                (updateDTO.getNextFollowUpDescription() != null && !updateDTO.getNextFollowUpDescription().isEmpty())) {
            String followUpChange = processFollowUpUpdate(lead, updateDTO);
            if (!followUpChange.isEmpty()) {
                changes.add(followUpChange);
                leadHistoryService.recordFollowUpUpdate(oldLead, lead, employee,
                        oldFollowUpDate, lead.getNextFollowUpDate().toString(),
                        oldFollowUpDesc, lead.getNextFollowUp());
            }
        }

        // 5. Handle conversion to customer
        if (Boolean.TRUE.equals(updateDTO.getConvertToCustomer())) {
            changes.add("Lead marked as converted to customer");
            log.info("Lead {} converted to customer by employee {}", leadId, employeeEmail);
        }

        // 6. Update remarks if provided
        if (updateDTO.getRemarks() != null && !updateDTO.getRemarks().isEmpty()) {
            changes.add("Remarks updated: " + updateDTO.getRemarks());
            lead.setRemarks(updateDTO.getRemarks());
        }

        // Update tracking fields
        lead.setUpdateCount(lead.getUpdateCount() + 1);
        lead.setLastUpdatedBy(employee.getEmail());

        // Save lead
        Lead savedLead = leadRepository.save(lead);

        // Record detailed history if there are changes
        if (!changes.isEmpty()) {
            // Record detailed field-level changes
            leadHistoryService.recordDetailedLeadUpdate(oldLead, savedLead, employee,
                    "Consolidated lead update by employee: " + employee.getEmail());

            // Send email notification to admin
            String employeeRole = employee.getEmail().equals("redcircle0908@gmail.com") ? "ADMIN" : "EMPLOYEE";
            emailService.sendLeadChangeNotificationToAdmin(oldLead, savedLead,
                    employee.getFirstName() + " " + employee.getLastName(),
                    employeeRole, changes);

            log.info("Recorded detailed history and sent notification for lead {} with {} changes", leadId, changes.size());
        }

        log.info("Lead {} updated successfully with {} changes", leadId, changes.size());
        return mapToResponseDTO(savedLead);
    }

    private Lead copyLead(Lead original) {
        if (original == null) return null;

        return Lead.builder()
                .id(original.getId())
                .name(original.getName())
                .email(original.getEmail())
                .phoneNumber(original.getPhoneNumber())
                .leadType(original.getLeadType())
                .leadStage(original.getLeadStage())
                .nextFollowUpDate(original.getNextFollowUpDate())
                .remarks(original.getRemarks())
                .nextFollowUp(original.getNextFollowUp())
                .assignedEmployee(original.getAssignedEmployee())
                .source(original.getSource())
                .isActive(original.getIsActive())
                .interestedService(original.getInterestedService())
                .serviceSubcategory(original.getServiceSubcategory())
                .serviceSubSubcategory(original.getServiceSubSubcategory())
                .serviceDescription(original.getServiceDescription())
                .callsMadeCount(original.getCallsMadeCount())
                .meetingsBookedCount(original.getMeetingsBookedCount())
                .meetingsDoneCount(original.getMeetingsDoneCount())
                .updateCount(original.getUpdateCount())
                .lastUpdatedBy(original.getLastUpdatedBy())
                .lastContactDate(original.getLastContactDate())
                .createdAt(original.getCreatedAt())
                .updatedAt(original.getUpdatedAt())
                .version(original.getVersion())
                .build();
    }

    private String processContactMade(Lead lead, EmployeeLeadUpdateDTO updateDTO) {
        StringBuilder contactInfo = new StringBuilder("Contact made. ");

        if (updateDTO.getResponseMessage() != null && !updateDTO.getResponseMessage().isEmpty()) {
            String response = updateDTO.getResponseMessage().toLowerCase();

            if (response.contains("call") || response.contains("phone")) {
                lead.setCallsMadeCount(lead.getCallsMadeCount() + 1);
                contactInfo.append("Phone call recorded. ");
            }

            contactInfo.append("Response: \"").append(updateDTO.getResponseMessage()).append("\". ");

            if (response.contains("interested") || response.contains("yes") || response.contains("want")) {
                if (lead.getLeadStage() != LeadStage.INTERESTED) {
                    contactInfo.append("Lead marked as INTERESTED based on response. ");
                    lead.setLeadStage(LeadStage.INTERESTED);
                }
            } else if (response.contains("not interested") || response.contains("no") || response.contains("don't want")) {
                if (lead.getLeadStage() != LeadStage.NOT_INTERESTED) {
                    contactInfo.append("Lead marked as NOT_INTERESTED based on response. ");
                    lead.setLeadStage(LeadStage.NOT_INTERESTED);
                }
            }
        }

        return contactInfo.toString();
    }

    private List<String> processStatisticsUpdates(Lead lead, EmployeeLeadUpdateDTO updateDTO) {
        List<String> statsChanges = new ArrayList<>();

        if (Boolean.TRUE.equals(updateDTO.getCallsMade())) {
            lead.setCallsMadeCount(lead.getCallsMadeCount() + 1);
            statsChanges.add("Phone call made (Total calls: " + lead.getCallsMadeCount() + ")");
        }

        if (Boolean.TRUE.equals(updateDTO.getMeetingBooked())) {
            lead.setMeetingsBookedCount(lead.getMeetingsBookedCount() + 1);
            statsChanges.add("Meeting booked (Total meetings booked: " + lead.getMeetingsBookedCount() + ")");

            if (lead.getLeadStage() != LeadStage.INTERESTED) {
                statsChanges.add("Stage automatically updated to INTERESTED (meeting booked)");
                lead.setLeadStage(LeadStage.INTERESTED);
            }
        }

        if (Boolean.TRUE.equals(updateDTO.getMeetingDone())) {
            lead.setMeetingsDoneCount(lead.getMeetingsDoneCount() + 1);
            statsChanges.add("Meeting completed (Total meetings done: " + lead.getMeetingsDoneCount() + ")");

            if (lead.getLeadStage() != LeadStage.INTERESTED) {
                statsChanges.add("Stage updated to INTERESTED (meeting completed)");
                lead.setLeadStage(LeadStage.INTERESTED);
            }
        }

        return statsChanges;
    }

    private String processFollowUpUpdate(Lead lead, EmployeeLeadUpdateDTO updateDTO) {
        StringBuilder followUpChange = new StringBuilder();

        if (updateDTO.getNextFollowUpDate() != null) {
            String oldDate = lead.getNextFollowUpDate().toString();
            lead.setNextFollowUpDate(updateDTO.getNextFollowUpDate());
            followUpChange.append("Follow-up date changed from ").append(oldDate)
                    .append(" to ").append(updateDTO.getNextFollowUpDate()).append(". ");
        }

        if (updateDTO.getNextFollowUpDescription() != null && !updateDTO.getNextFollowUpDescription().isEmpty()) {
            String oldDescription = lead.getNextFollowUp();
            lead.setNextFollowUp(updateDTO.getNextFollowUpDescription());
            followUpChange.append("Follow-up description updated from \"").append(oldDescription)
                    .append("\" to \"").append(updateDTO.getNextFollowUpDescription()).append("\". ");
        }

        return followUpChange.toString();
    }

    private boolean isValidLeadStage(LeadStage stage) {
        return stage == LeadStage.INTERESTED ||
                stage == LeadStage.NOT_INTERESTED ||
                stage == LeadStage.NORMAL;
    }

    private LeadResponseDTO mapToResponseDTO(Lead lead) {
        LeadResponseDTO dto = new LeadResponseDTO();
        dto.setId(lead.getId());
        dto.setName(lead.getName());
        dto.setEmail(lead.getEmail());
        dto.setPhoneNumber(lead.getPhoneNumber());
        dto.setLeadType(lead.getLeadType());
        dto.setLeadStage(lead.getLeadStage());
        dto.setNextFollowUpDate(lead.getNextFollowUpDate());
        dto.setRemarks(lead.getRemarks());
        dto.setNextFollowUp(lead.getNextFollowUp());
        dto.setSource(lead.getSource());
        dto.setIsActive(lead.getIsActive());
        dto.setCallsMadeCount(lead.getCallsMadeCount());
        dto.setMeetingsBookedCount(lead.getMeetingsBookedCount());
        dto.setMeetingsDoneCount(lead.getMeetingsDoneCount());
        dto.setInterestedService(lead.getInterestedService());
        dto.setServiceSubcategory(lead.getServiceSubcategory());
        dto.setServiceSubSubcategory(lead.getServiceSubSubcategory());
        dto.setServiceDescription(lead.getServiceDescription());
        dto.setCreatedAt(lead.getCreatedAt());
        dto.setUpdatedAt(lead.getUpdatedAt());
        dto.setUpdateCount(lead.getUpdateCount());
        dto.setLastUpdatedBy(lead.getLastUpdatedBy());
        dto.setLastContactDate(lead.getLastContactDate());

        if (lead.getAssignedEmployee() != null) {
            EmployeeResponseDTO empDto = new EmployeeResponseDTO();
            empDto.setId(lead.getAssignedEmployee().getId());
            empDto.setFirstName(lead.getAssignedEmployee().getFirstName());
            empDto.setLastName(lead.getAssignedEmployee().getLastName());
            empDto.setEmail(lead.getAssignedEmployee().getEmail());
            empDto.setEmployeeCode(lead.getAssignedEmployee().getEmployeeCode());
            empDto.setDepartment(lead.getAssignedEmployee().getDepartment());
            empDto.setPosition(lead.getAssignedEmployee().getPosition());
            dto.setAssignedEmployee(empDto);
        }

        return dto;
    }
}