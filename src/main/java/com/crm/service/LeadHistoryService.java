package com.crm.service;

import com.crm.dto.LeadHistoryDTO;
import com.crm.dto.EmployeeBasicDTO;
import com.crm.entity.Employee;
import com.crm.entity.Lead;
import com.crm.entity.LeadHistory;
import com.crm.repository.LeadHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeadHistoryService {

    private final LeadHistoryRepository leadHistoryRepository;
    private final ObjectMapper objectMapper;

    // Remove the manual constructor - let Lombok generate it
    // The ObjectMapper is now injected as a bean

    public void recordLeadCreation(Lead lead, Employee employee) {
        Map<String, Object> leadDetails = new LinkedHashMap<>();
        leadDetails.put("name", lead.getName());
        leadDetails.put("email", lead.getEmail());
        leadDetails.put("phoneNumber", lead.getPhoneNumber());
        leadDetails.put("leadType", lead.getLeadType() != null ? lead.getLeadType().toString() : "Not set");
        leadDetails.put("leadStage", lead.getLeadStage() != null ? lead.getLeadStage().toString() : "Not set");
        leadDetails.put("source", lead.getSource());

        try {
            LeadHistory history = LeadHistory.builder()
                    .lead(lead)
                    .employee(employee)
                    .action("CREATE")
                    .changes("Lead created with details: " + objectMapper.writeValueAsString(leadDetails))
                    .oldValues("{}")
                    .newValues(objectMapper.writeValueAsString(leadDetails))
                    .remarks("Initial lead creation")
                    .previousStage("NONE")
                    .newStage(lead.getLeadStage() != null ? lead.getLeadStage().toString() : "NONE")
                    .build();
            leadHistoryRepository.save(history);
            log.info("Recorded lead creation history for lead ID: {}", lead.getId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing lead details", e);
        }
    }

    public void recordDetailedLeadUpdate(Lead oldLead, Lead newLead, Employee employee, String remarks) {
        Map<String, Object> oldValues = new LinkedHashMap<>();
        Map<String, Object> newValues = new LinkedHashMap<>();
        List<String> changeList = new ArrayList<>();

        // Compare all fields
        compareField("Name", oldLead.getName(), newLead.getName(), oldValues, newValues, changeList);
        compareField("Email", oldLead.getEmail(), newLead.getEmail(), oldValues, newValues, changeList);
        compareField("Phone Number", oldLead.getPhoneNumber(), newLead.getPhoneNumber(), oldValues, newValues, changeList);
        compareField("Lead Type",
                oldLead.getLeadType() != null ? oldLead.getLeadType().toString() : null,
                newLead.getLeadType() != null ? newLead.getLeadType().toString() : null,
                oldValues, newValues, changeList);
        compareField("Lead Stage",
                oldLead.getLeadStage() != null ? oldLead.getLeadStage().toString() : null,
                newLead.getLeadStage() != null ? newLead.getLeadStage().toString() : null,
                oldValues, newValues, changeList);
        compareField("Next Follow-up Date", oldLead.getNextFollowUpDate(), newLead.getNextFollowUpDate(), oldValues, newValues, changeList);
        compareField("Remarks", oldLead.getRemarks(), newLead.getRemarks(), oldValues, newValues, changeList);
        compareField("Next Follow-up Description", oldLead.getNextFollowUp(), newLead.getNextFollowUp(), oldValues, newValues, changeList);
        compareField("Source", oldLead.getSource(), newLead.getSource(), oldValues, newValues, changeList);
        compareField("Assigned Employee",
                oldLead.getAssignedEmployee() != null ? oldLead.getAssignedEmployee().getEmail() : "None",
                newLead.getAssignedEmployee() != null ? newLead.getAssignedEmployee().getEmail() : "None",
                oldValues, newValues, changeList);
        compareField("Interested Service",
                oldLead.getInterestedService() != null ? oldLead.getInterestedService().getDisplayName() : null,
                newLead.getInterestedService() != null ? newLead.getInterestedService().getDisplayName() : null,
                oldValues, newValues, changeList);
        compareField("Service Subcategory",
                oldLead.getServiceSubcategory() != null ? oldLead.getServiceSubcategory().getDisplayName() : null,
                newLead.getServiceSubcategory() != null ? newLead.getServiceSubcategory().getDisplayName() : null,
                oldValues, newValues, changeList);
        compareField("Service Sub-subcategory",
                oldLead.getServiceSubSubcategory() != null ? oldLead.getServiceSubSubcategory().getDisplayName() : null,
                newLead.getServiceSubSubcategory() != null ? newLead.getServiceSubSubcategory().getDisplayName() : null,
                oldValues, newValues, changeList);
        compareField("Service Description", oldLead.getServiceDescription(), newLead.getServiceDescription(), oldValues, newValues, changeList);
        compareField("Calls Made Count", oldLead.getCallsMadeCount(), newLead.getCallsMadeCount(), oldValues, newValues, changeList);
        compareField("Meetings Booked Count", oldLead.getMeetingsBookedCount(), newLead.getMeetingsBookedCount(), oldValues, newValues, changeList);
        compareField("Meetings Done Count", oldLead.getMeetingsDoneCount(), newLead.getMeetingsDoneCount(), oldValues, newValues, changeList);

        if (changeList.isEmpty()) {
            log.info("No changes detected for lead ID: {}", oldLead.getId());
            return;
        }

        try {
            LeadHistory history = LeadHistory.builder()
                    .lead(newLead)
                    .employee(employee)
                    .action("UPDATE")
                    .changes(String.join("; ", changeList))
                    .oldValues(objectMapper.writeValueAsString(oldValues))
                    .newValues(objectMapper.writeValueAsString(newValues))
                    .remarks(remarks != null ? remarks : "Lead details updated by " + employee.getEmail())
                    .previousStage(oldLead.getLeadStage() != null ? oldLead.getLeadStage().toString() : "NONE")
                    .newStage(newLead.getLeadStage() != null ? newLead.getLeadStage().toString() : "NONE")
                    .build();
            leadHistoryRepository.save(history);
            log.info("Recorded detailed lead update history for lead ID: {} with {} changes", oldLead.getId(), changeList.size());
        } catch (JsonProcessingException e) {
            log.error("Error serializing lead changes", e);
        }
    }

    public void recordStageChange(Lead lead, Employee employee, String previousStage, String newStage, String remarks) {
        Map<String, Object> oldValues = new LinkedHashMap<>();
        Map<String, Object> newValues = new LinkedHashMap<>();
        oldValues.put("leadStage", previousStage);
        newValues.put("leadStage", newStage);

        try {
            LeadHistory history = LeadHistory.builder()
                    .lead(lead)
                    .employee(employee)
                    .action("STAGE_CHANGE")
                    .changes(String.format("Stage changed from '%s' to '%s'", previousStage, newStage))
                    .oldValues(objectMapper.writeValueAsString(oldValues))
                    .newValues(objectMapper.writeValueAsString(newValues))
                    .remarks(remarks != null ? remarks : "Stage updated by " + employee.getEmail())
                    .previousStage(previousStage)
                    .newStage(newStage)
                    .build();
            leadHistoryRepository.save(history);
            log.info("Recorded stage change history for lead ID: {} from {} to {}", lead.getId(), previousStage, newStage);
        } catch (JsonProcessingException e) {
            log.error("Error serializing stage change", e);
        }
    }

    public void recordStatisticsUpdate(Lead oldLead, Lead newLead, Employee employee, List<String> statsChanges) {
        Map<String, Object> oldStats = new LinkedHashMap<>();
        Map<String, Object> newStats = new LinkedHashMap<>();

        if (!Objects.equals(oldLead.getCallsMadeCount(), newLead.getCallsMadeCount())) {
            oldStats.put("callsMadeCount", oldLead.getCallsMadeCount());
            newStats.put("callsMadeCount", newLead.getCallsMadeCount());
        }
        if (!Objects.equals(oldLead.getMeetingsBookedCount(), newLead.getMeetingsBookedCount())) {
            oldStats.put("meetingsBookedCount", oldLead.getMeetingsBookedCount());
            newStats.put("meetingsBookedCount", newLead.getMeetingsBookedCount());
        }
        if (!Objects.equals(oldLead.getMeetingsDoneCount(), newLead.getMeetingsDoneCount())) {
            oldStats.put("meetingsDoneCount", oldLead.getMeetingsDoneCount());
            newStats.put("meetingsDoneCount", newLead.getMeetingsDoneCount());
        }

        try {
            LeadHistory history = LeadHistory.builder()
                    .lead(newLead)
                    .employee(employee)
                    .action("STATISTICS_UPDATE")
                    .changes(String.join("; ", statsChanges))
                    .oldValues(objectMapper.writeValueAsString(oldStats))
                    .newValues(objectMapper.writeValueAsString(newStats))
                    .remarks("Statistics updated by " + employee.getEmail())
                    .previousStage(oldLead.getLeadStage() != null ? oldLead.getLeadStage().toString() : "NONE")
                    .newStage(newLead.getLeadStage() != null ? newLead.getLeadStage().toString() : "NONE")
                    .build();
            leadHistoryRepository.save(history);
            log.info("Recorded statistics update history for lead ID: {}", newLead.getId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing statistics update", e);
        }
    }

    public void recordFollowUpUpdate(Lead oldLead, Lead newLead, Employee employee,
                                     String oldFollowUpDate, String newFollowUpDate,
                                     String oldDescription, String newDescription) {
        Map<String, Object> oldValues = new LinkedHashMap<>();
        Map<String, Object> newValues = new LinkedHashMap<>();

        oldValues.put("nextFollowUpDate", oldFollowUpDate);
        newValues.put("nextFollowUpDate", newFollowUpDate);
        oldValues.put("nextFollowUp", oldDescription);
        newValues.put("nextFollowUp", newDescription);

        String changes = String.format("Follow-up date: %s → %s; Description: %s",
                oldFollowUpDate, newFollowUpDate, newDescription);

        try {
            LeadHistory history = LeadHistory.builder()
                    .lead(newLead)
                    .employee(employee)
                    .action("FOLLOWUP_UPDATE")
                    .changes(changes)
                    .oldValues(objectMapper.writeValueAsString(oldValues))
                    .newValues(objectMapper.writeValueAsString(newValues))
                    .remarks("Follow-up rescheduled by " + employee.getEmail())
                    .previousStage(oldLead.getLeadStage() != null ? oldLead.getLeadStage().toString() : "NONE")
                    .newStage(newLead.getLeadStage() != null ? newLead.getLeadStage().toString() : "NONE")
                    .build();
            leadHistoryRepository.save(history);
            log.info("Recorded follow-up update history for lead ID: {}", newLead.getId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing follow-up update", e);
        }
    }

    public void recordContactMade(Lead lead, Employee employee, String contactMethod, String response, String remarks) {
        Map<String, Object> contactDetails = new LinkedHashMap<>();
        contactDetails.put("contactMethod", contactMethod);
        contactDetails.put("response", response);
        contactDetails.put("contactDate", LocalDateTime.now());

        try {
            LeadHistory history = LeadHistory.builder()
                    .lead(lead)
                    .employee(employee)
                    .action("CONTACT_MADE")
                    .changes(String.format("Contact made via %s. Response: %s", contactMethod, response))
                    .oldValues("{}")
                    .newValues(objectMapper.writeValueAsString(contactDetails))
                    .remarks(remarks != null ? remarks : "Contact made by " + employee.getEmail())
                    .previousStage(lead.getLeadStage() != null ? lead.getLeadStage().toString() : "NONE")
                    .newStage(lead.getLeadStage() != null ? lead.getLeadStage().toString() : "NONE")
                    .build();
            leadHistoryRepository.save(history);
            log.info("Recorded contact made history for lead ID: {} via {}", lead.getId(), contactMethod);
        } catch (JsonProcessingException e) {
            log.error("Error serializing contact details", e);
        }
    }

    private void compareField(String fieldName, Object oldValue, Object newValue,
                              Map<String, Object> oldValues, Map<String, Object> newValues,
                              List<String> changeList) {
        String oldStr = oldValue != null ? oldValue.toString() : "";
        String newStr = newValue != null ? newValue.toString() : "";

        if (!oldStr.equals(newStr)) {
            oldValues.put(fieldName, oldStr.isEmpty() ? "Not set" : oldStr);
            newValues.put(fieldName, newStr.isEmpty() ? "Not set" : newStr);
            changeList.add(String.format("%s: '%s' → '%s'", fieldName,
                    oldStr.isEmpty() ? "Not set" : oldStr,
                    newStr.isEmpty() ? "Not set" : newStr));
        }
    }

    public List<LeadHistoryDTO> getLeadHistory(Long leadId) {
        return leadHistoryRepository.findByLeadIdOrderByCreatedAtDesc(leadId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<LeadHistoryDTO> getEmployeeLeadHistory(Long employeeId) {
        return leadHistoryRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private LeadHistoryDTO mapToDTO(LeadHistory history) {
        LeadHistoryDTO dto = new LeadHistoryDTO();
        dto.setId(history.getId());
        dto.setAction(history.getAction());
        dto.setChanges(history.getChanges());
        dto.setOldValues(history.getOldValues());
        dto.setNewValues(history.getNewValues());
        dto.setRemarks(history.getRemarks());
        dto.setPreviousStage(history.getPreviousStage());
        dto.setNewStage(history.getNewStage());
        dto.setCreatedAt(history.getCreatedAt());

        // Parse detailed changes if available
        if (history.getOldValues() != null && history.getNewValues() != null) {
            try {
                Map<String, Object> oldMap = objectMapper.readValue(history.getOldValues(), Map.class);
                Map<String, Object> newMap = objectMapper.readValue(history.getNewValues(), Map.class);

                Map<String, LeadHistoryDTO.FieldChangeDTO> detailedChanges = new LinkedHashMap<>();
                for (String key : newMap.keySet()) {
                    LeadHistoryDTO.FieldChangeDTO fieldChange = new LeadHistoryDTO.FieldChangeDTO();
                    fieldChange.setFieldName(key);
                    fieldChange.setOldValue(oldMap.get(key) != null ? oldMap.get(key).toString() : "Not set");
                    fieldChange.setNewValue(newMap.get(key) != null ? newMap.get(key).toString() : "Not set");
                    detailedChanges.put(key, fieldChange);
                }
                dto.setDetailedChanges(detailedChanges);
            } catch (Exception e) {
                log.error("Error parsing detailed changes", e);
            }
        }

        if (history.getEmployee() != null) {
            EmployeeBasicDTO employeeDTO = new EmployeeBasicDTO();
            employeeDTO.setId(history.getEmployee().getId());
            employeeDTO.setFirstName(history.getEmployee().getFirstName());
            employeeDTO.setLastName(history.getEmployee().getLastName());
            employeeDTO.setEmail(history.getEmployee().getEmail());
            dto.setEmployee(employeeDTO);
        }

        return dto;
    }
}