package com.crm.service;

import com.crm.dto.*;
import com.crm.entity.Employee;
import com.crm.entity.Lead;
import com.crm.entity.LeadStage;
import com.crm.entity.LeadType;
import com.crm.exception.DuplicateResourceException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.exception.UnauthorizedException;
import com.crm.repository.EmployeeRepository;
import com.crm.repository.LeadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;
    private final LeadHistoryService leadHistoryService;
    private final ObjectMapper objectMapper;

    @Override
    public LeadResponseDTO createLead(LeadDTO leadDTO) {
        log.info("Creating new lead with email: {}", leadDTO.getEmail());

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee currentUser = employeeRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (leadRepository.existsByEmail(leadDTO.getEmail())) {
            throw new DuplicateResourceException("Lead with email already exists: " + leadDTO.getEmail());
        }

        Employee assignedEmployee = employeeRepository.findById(leadDTO.getAssignedEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + leadDTO.getAssignedEmployeeId()));

        Lead lead = mapToEntity(leadDTO, assignedEmployee);
        Lead savedLead = leadRepository.save(lead);

        leadHistoryService.recordLeadCreation(savedLead, currentUser);

        emailService.sendLeadAssignmentEmail(
                assignedEmployee.getEmail(),
                assignedEmployee.getFirstName() + " " + assignedEmployee.getLastName(),
                lead.getName(),
                lead.getLeadType().getDescription()
        );

        log.info("Lead created successfully with ID: {}", savedLead.getId());
        return mapToResponseDTO(savedLead);
    }

    @Override
    public LeadResponseDTO updateLead(Long id, LeadUpdateDTO leadUpdateDTO) {
        log.info("Updating lead with ID: {}", id);

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee currentUser = employeeRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Lead existingLead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));

        // Create a copy of lead before changes for comparison
        Lead oldLead = copyLead(existingLead);
        List<String> changes = new ArrayList<>();

        // Track and update each field
        if (leadUpdateDTO.getName() != null && !leadUpdateDTO.getName().equals(existingLead.getName())) {
            changes.add("Name: '" + existingLead.getName() + "' → '" + leadUpdateDTO.getName() + "'");
            existingLead.setName(leadUpdateDTO.getName());
        }

        if (leadUpdateDTO.getEmail() != null && !leadUpdateDTO.getEmail().equals(existingLead.getEmail())) {
            if (leadRepository.existsByEmail(leadUpdateDTO.getEmail())) {
                throw new DuplicateResourceException("Lead with email already exists: " + leadUpdateDTO.getEmail());
            }
            changes.add("Email: '" + existingLead.getEmail() + "' → '" + leadUpdateDTO.getEmail() + "'");
            existingLead.setEmail(leadUpdateDTO.getEmail());
        }

        if (leadUpdateDTO.getPhoneNumber() != null && !leadUpdateDTO.getPhoneNumber().equals(existingLead.getPhoneNumber())) {
            changes.add("Phone: '" + existingLead.getPhoneNumber() + "' → '" + leadUpdateDTO.getPhoneNumber() + "'");
            existingLead.setPhoneNumber(leadUpdateDTO.getPhoneNumber());
        }

        if (leadUpdateDTO.getLeadType() != null && leadUpdateDTO.getLeadType() != existingLead.getLeadType()) {
            changes.add("Lead Type: '" + existingLead.getLeadType() + "' → '" + leadUpdateDTO.getLeadType() + "'");
            existingLead.setLeadType(leadUpdateDTO.getLeadType());
        }

        if (leadUpdateDTO.getLeadStage() != null && leadUpdateDTO.getLeadStage() != existingLead.getLeadStage()) {
            String previousStage = existingLead.getLeadStage().toString();
            String newStage = leadUpdateDTO.getLeadStage().toString();
            changes.add("Stage: '" + previousStage + "' → '" + newStage + "'");
            leadHistoryService.recordStageChange(existingLead, currentUser, previousStage, newStage, "Stage updated by admin");
            existingLead.setLeadStage(leadUpdateDTO.getLeadStage());
        }

        if (leadUpdateDTO.getNextFollowUpDate() != null && !leadUpdateDTO.getNextFollowUpDate().equals(existingLead.getNextFollowUpDate())) {
            changes.add("Follow-up Date: '" + existingLead.getNextFollowUpDate() + "' → '" + leadUpdateDTO.getNextFollowUpDate() + "'");
            existingLead.setNextFollowUpDate(leadUpdateDTO.getNextFollowUpDate());
        }

        if (leadUpdateDTO.getRemarks() != null && !leadUpdateDTO.getRemarks().equals(existingLead.getRemarks())) {
            changes.add("Remarks: '" + existingLead.getRemarks() + "' → '" + leadUpdateDTO.getRemarks() + "'");
            existingLead.setRemarks(leadUpdateDTO.getRemarks());
        }

        if (leadUpdateDTO.getNextFollowUp() != null && !leadUpdateDTO.getNextFollowUp().equals(existingLead.getNextFollowUp())) {
            changes.add("Follow-up description: '" + existingLead.getNextFollowUp() + "' → '" + leadUpdateDTO.getNextFollowUp() + "'");
            existingLead.setNextFollowUp(leadUpdateDTO.getNextFollowUp());
        }

        if (leadUpdateDTO.getSource() != null && !leadUpdateDTO.getSource().equals(existingLead.getSource())) {
            changes.add("Source: '" + existingLead.getSource() + "' → '" + leadUpdateDTO.getSource() + "'");
            existingLead.setSource(leadUpdateDTO.getSource());
        }

        if (leadUpdateDTO.getAssignedEmployeeId() != null && !leadUpdateDTO.getAssignedEmployeeId().equals(existingLead.getAssignedEmployee().getId())) {
            Employee newEmployee = employeeRepository.findById(leadUpdateDTO.getAssignedEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + leadUpdateDTO.getAssignedEmployeeId()));
            changes.add("Assigned Employee: '" + existingLead.getAssignedEmployee().getEmail() + "' → '" + newEmployee.getEmail() + "'");
            existingLead.setAssignedEmployee(newEmployee);
            emailService.sendLeadAssignmentEmail(
                    newEmployee.getEmail(),
                    newEmployee.getFirstName() + " " + newEmployee.getLastName(),
                    existingLead.getName(),
                    existingLead.getLeadType().getDescription()
            );
        }

        // Update service-related fields
        if (leadUpdateDTO.getInterestedService() != null) {
            if (!leadUpdateDTO.getInterestedService().equals(existingLead.getInterestedService())) {
                changes.add("Interested Service: '" + (existingLead.getInterestedService() != null ? existingLead.getInterestedService().getDisplayName() : "Not set") +
                        "' → '" + leadUpdateDTO.getInterestedService().getDisplayName() + "'");
                existingLead.setInterestedService(leadUpdateDTO.getInterestedService());
            }
        }

        if (leadUpdateDTO.getServiceSubcategory() != null) {
            if (!leadUpdateDTO.getServiceSubcategory().equals(existingLead.getServiceSubcategory())) {
                changes.add("Service Subcategory: '" + (existingLead.getServiceSubcategory() != null ? existingLead.getServiceSubcategory().getDisplayName() : "Not set") +
                        "' → '" + leadUpdateDTO.getServiceSubcategory().getDisplayName() + "'");
                existingLead.setServiceSubcategory(leadUpdateDTO.getServiceSubcategory());
            }
        }

        if (leadUpdateDTO.getServiceSubSubcategory() != null) {
            if (!leadUpdateDTO.getServiceSubSubcategory().equals(existingLead.getServiceSubSubcategory())) {
                changes.add("Service Sub-subcategory: '" + (existingLead.getServiceSubSubcategory() != null ? existingLead.getServiceSubSubcategory().getDisplayName() : "Not set") +
                        "' → '" + leadUpdateDTO.getServiceSubSubcategory().getDisplayName() + "'");
                existingLead.setServiceSubSubcategory(leadUpdateDTO.getServiceSubSubcategory());
            }
        }

        if (leadUpdateDTO.getServiceDescription() != null && !leadUpdateDTO.getServiceDescription().equals(existingLead.getServiceDescription())) {
            changes.add("Service Description updated");
            existingLead.setServiceDescription(leadUpdateDTO.getServiceDescription());
        }

        existingLead.setUpdateCount(existingLead.getUpdateCount() + 1);
        existingLead.setLastUpdatedBy(currentUser.getEmail());

        Lead updatedLead = leadRepository.save(existingLead);

        if (!changes.isEmpty()) {
            // Record detailed field-level changes
            leadHistoryService.recordDetailedLeadUpdate(oldLead, updatedLead, currentUser,
                    "Lead details updated by admin: " + currentUser.getEmail());

            // Send email notification to admin
            String role = currentUser.getEmail().equals("redcircle0908@gmail.com") ? "ADMIN" : "EMPLOYEE";
            emailService.sendLeadChangeNotificationToAdmin(oldLead, updatedLead,
                    currentUser.getFirstName() + " " + currentUser.getLastName(),
                    role, changes);

            log.info("Recorded detailed history and sent notification for lead {} with {} changes", id, changes.size());
        }

        log.info("Lead updated successfully with ID: {}", id);
        return mapToResponseDTO(updatedLead);
    }

    @Override
    public void deleteLead(Long id) {
        log.info("Soft deleting lead with ID: {}", id);

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee currentUser = employeeRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));

        // Create a copy for history
        Lead oldLead = copyLead(lead);

        lead.setIsActive(false);
        lead.setLastUpdatedBy(currentUser.getEmail());
        Lead savedLead = leadRepository.save(lead);

        // Record deletion in history
        leadHistoryService.recordDetailedLeadUpdate(oldLead, savedLead, currentUser,
                "Lead soft deleted by " + currentUser.getEmail());

        log.info("Lead soft deleted successfully with ID: {}", id);
    }

    @Override
    public LeadResponseDTO getLeadById(Long id) {
        log.info("Fetching lead with ID: {}", id);
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));
        return mapToResponseDTO(lead);
    }

    @Override
    public List<LeadResponseDTO> getAllLeads() {
        log.info("Fetching all active leads");
        return leadRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeadResponseDTO> getLeadsByEmployee(Long employeeId) {
        log.info("Fetching leads for employee ID: {}", employeeId);
        return leadRepository.findByAssignedEmployeeId(employeeId)
                .stream()
                .filter(Lead::getIsActive)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeadResponseDTO> getLeadsByType(LeadType leadType) {
        log.info("Fetching leads by type: {}", leadType);
        return leadRepository.findByLeadType(leadType)
                .stream()
                .filter(Lead::getIsActive)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeadResponseDTO> getLeadsByStage(LeadStage leadStage) {
        log.info("Fetching leads by stage: {}", leadStage);
        return leadRepository.findByLeadStage(leadStage)
                .stream()
                .filter(Lead::getIsActive)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeadResponseDTO> getTodayFollowUps() {
        log.info("Fetching today's follow-ups");
        return leadRepository.findByNextFollowUpDate(LocalDate.now())
                .stream()
                .filter(Lead::getIsActive)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeadResponseDTO> getPendingFollowUps() {
        log.info("Fetching pending follow-ups");
        return leadRepository.findByNextFollowUpDateBefore(LocalDate.now())
                .stream()
                .filter(Lead::getIsActive)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public LeadResponseDTO updateLeadStatistics(Long id, LeadStatisticsUpdateDTO statisticsDTO) {
        log.info("Updating statistics for lead ID: {}", id);

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee currentUser = employeeRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));

        boolean isAdmin = currentUserEmail.equals("redcircle0908@gmail.com");
        boolean isAssignedEmployee = lead.getAssignedEmployee() != null &&
                lead.getAssignedEmployee().getId().equals(currentUser.getId());

        if (!isAdmin && !isAssignedEmployee) {
            throw new UnauthorizedException("You are not authorized to update this lead");
        }

        // Create a copy for history
        Lead oldLead = copyLead(lead);
        List<String> statsChanges = new ArrayList<>();

        if (Boolean.TRUE.equals(statisticsDTO.getCallsMade())) {
            lead.setCallsMadeCount(lead.getCallsMadeCount() + 1);
            statsChanges.add("Phone call made (Total: " + lead.getCallsMadeCount() + ")");
            lead.setLastContactDate(LocalDateTime.now());
        }

        if (Boolean.TRUE.equals(statisticsDTO.getMeetingBooked())) {
            lead.setMeetingsBookedCount(lead.getMeetingsBookedCount() + 1);
            statsChanges.add("Meeting booked (Total: " + lead.getMeetingsBookedCount() + ")");
        }

        if (Boolean.TRUE.equals(statisticsDTO.getMeetingDone())) {
            lead.setMeetingsDoneCount(lead.getMeetingsDoneCount() + 1);
            statsChanges.add("Meeting completed (Total: " + lead.getMeetingsDoneCount() + ")");
        }

        lead.setUpdateCount(lead.getUpdateCount() + 1);
        lead.setLastUpdatedBy(currentUser.getEmail());

        Lead updatedLead = leadRepository.save(lead);

        if (!statsChanges.isEmpty()) {
            String allChanges = String.join("; ", statsChanges);
            leadHistoryService.recordStatisticsUpdate(oldLead, updatedLead, currentUser, statsChanges);

            // Send email notification for statistics update
            emailService.sendLeadChangeNotificationToAdmin(oldLead, updatedLead,
                    currentUser.getFirstName() + " " + currentUser.getLastName(),
                    isAdmin ? "ADMIN" : "EMPLOYEE", statsChanges);
        }

        log.info("Statistics updated for lead {} by {}", id, currentUser.getEmail());
        return mapToResponseDTO(updatedLead);
    }

    @Override
    public Map<String, Long> getLeadStatistics() {
        log.info("Fetching lead statistics");
        Map<String, Long> statistics = new HashMap<>();

        List<Lead> allLeads = leadRepository.findByIsActiveTrue();

        statistics.put("total_leads", (long) allLeads.size());
        statistics.put("hot_leads", allLeads.stream().filter(l -> l.getLeadType() == LeadType.HOT).count());
        statistics.put("warm_leads", allLeads.stream().filter(l -> l.getLeadType() == LeadType.WARM).count());
        statistics.put("cold_leads", allLeads.stream().filter(l -> l.getLeadType() == LeadType.COLD).count());
        statistics.put("interested_leads", allLeads.stream().filter(l -> l.getLeadStage() == LeadStage.INTERESTED).count());
        statistics.put("not_interested_leads", allLeads.stream().filter(l -> l.getLeadStage() == LeadStage.NOT_INTERESTED).count());
        statistics.put("normal_leads", allLeads.stream().filter(l -> l.getLeadStage() == LeadStage.NORMAL).count());

        return statistics;
    }

    @Override
    public List<LeadResponseDTO> getLeadsByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching leads between {} and {}", startDate, endDate);
        return leadRepository.findAll()
                .stream()
                .filter(lead -> lead.getCreatedAt().toLocalDate().isAfter(startDate.minusDays(1)) &&
                        lead.getCreatedAt().toLocalDate().isBefore(endDate.plusDays(1)) &&
                        lead.getIsActive())
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public LeadResponseDTO updateLeadStage(Long id, String stage, String employeeEmail) {
        log.info("Updating lead stage for lead ID: {} to {}", id, stage);

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));

        if (!lead.getAssignedEmployee().getId().equals(employee.getId())) {
            throw new UnauthorizedException("You are not authorized to update this lead");
        }

        // Create a copy for history
        Lead oldLead = copyLead(lead);

        String oldStage = lead.getLeadStage().toString();
        LeadStage newStage;
        try {
            newStage = LeadStage.valueOf(stage.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid lead stage: " + stage + ". Valid stages: INTERESTED, NOT_INTERESTED, NORMAL");
        }

        lead.setLeadStage(newStage);
        lead.setUpdateCount(lead.getUpdateCount() + 1);
        lead.setLastUpdatedBy(employee.getEmail());

        Lead savedLead = leadRepository.save(lead);
        leadHistoryService.recordStageChange(savedLead, employee, oldStage, stage, "Stage updated by " + employee.getEmail());

        // Send email notification for stage change
        List<String> changes = List.of("Stage changed from '" + oldStage + "' to '" + stage + "'");
        emailService.sendLeadChangeNotificationToAdmin(oldLead, savedLead,
                employee.getFirstName() + " " + employee.getLastName(),
                "EMPLOYEE", changes);

        log.info("Stage updated for lead {} to {} by {}", id, newStage, employeeEmail);
        return mapToResponseDTO(savedLead);
    }

    @Override
    public LeadResponseDTO updateFollowUp(Long id, String nextFollowUpDate, String nextFollowUpDescription, String employeeEmail) {
        log.info("Updating follow-up for lead ID: {}", id);

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));

        if (!lead.getAssignedEmployee().getId().equals(employee.getId())) {
            throw new UnauthorizedException("You are not authorized to update this lead");
        }

        // Create a copy for history
        Lead oldLead = copyLead(lead);

        String oldFollowUpDate = lead.getNextFollowUpDate().toString();
        String oldDescription = lead.getNextFollowUp();
        LocalDate newDate = LocalDate.parse(nextFollowUpDate, DateTimeFormatter.ISO_LOCAL_DATE);

        lead.setNextFollowUpDate(newDate);
        lead.setNextFollowUp(nextFollowUpDescription);
        lead.setUpdateCount(lead.getUpdateCount() + 1);
        lead.setLastUpdatedBy(employee.getEmail());

        Lead savedLead = leadRepository.save(lead);
        leadHistoryService.recordFollowUpUpdate(oldLead, savedLead, employee,
                oldFollowUpDate, nextFollowUpDate, oldDescription, nextFollowUpDescription);

        // Send email notification for follow-up update
        List<String> changes = List.of(
                "Follow-up date changed from '" + oldFollowUpDate + "' to '" + nextFollowUpDate + "'",
                "Follow-up description changed from '" + oldDescription + "' to '" + nextFollowUpDescription + "'"
        );
        emailService.sendLeadChangeNotificationToAdmin(oldLead, savedLead,
                employee.getFirstName() + " " + employee.getLastName(),
                "EMPLOYEE", changes);

        log.info("Follow-up updated for lead {} for date {} by {}", id, newDate, employeeEmail);
        return mapToResponseDTO(savedLead);
    }

    @Override
    public LeadResponseDTO updateLeadAfterContact(Long id, EmployeeLeadUpdateDTO updateDTO, String employeeEmail) {
        log.info("Updating lead after contact for lead ID: {}", id);

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));

        if (!lead.getAssignedEmployee().getId().equals(employee.getId())) {
            throw new UnauthorizedException("You are not authorized to update this lead");
        }

        // Create a copy for history
        Lead oldLead = copyLead(lead);
        List<String> changes = new ArrayList<>();
        String oldStage = lead.getLeadStage().toString();

        if (Boolean.TRUE.equals(updateDTO.getContactMade())) {
            String contactInfo = processContactMade(lead, updateDTO);
            changes.add(contactInfo);
            lead.setLastContactDate(LocalDateTime.now());
            leadHistoryService.recordContactMade(lead, employee, "Phone/Email",
                    updateDTO.getResponseMessage() != null ? updateDTO.getResponseMessage() : "Contact attempted",
                    updateDTO.getRemarks());
        }

        List<String> statsChanges = processStatisticsUpdates(lead, updateDTO);
        changes.addAll(statsChanges);

        if (updateDTO.getNewLeadStage() != null && updateDTO.getNewLeadStage() != lead.getLeadStage()) {
            changes.add("Stage changed from " + oldStage + " to " + updateDTO.getNewLeadStage());
            leadHistoryService.recordStageChange(lead, employee, oldStage, updateDTO.getNewLeadStage().toString(), "Stage updated by employee");
            lead.setLeadStage(updateDTO.getNewLeadStage());
        }

        if (updateDTO.getNextFollowUpDate() != null ||
                (updateDTO.getNextFollowUpDescription() != null && !updateDTO.getNextFollowUpDescription().isEmpty())) {
            String followUpChange = processFollowUpUpdate(lead, updateDTO);
            changes.add(followUpChange);
        }

        if (Boolean.TRUE.equals(updateDTO.getConvertToCustomer())) {
            changes.add("Lead converted to customer");
            log.info("Lead {} converted to customer by employee {}", id, employeeEmail);
        }

        if (updateDTO.getRemarks() != null && !updateDTO.getRemarks().isEmpty()) {
            changes.add("Remarks updated: " + updateDTO.getRemarks());
            lead.setRemarks(updateDTO.getRemarks());
        }

        lead.setUpdateCount(lead.getUpdateCount() + 1);
        lead.setLastUpdatedBy(employee.getEmail());

        Lead savedLead = leadRepository.save(lead);

        if (!changes.isEmpty()) {
            String allChanges = String.join("; ", changes);
            leadHistoryService.recordDetailedLeadUpdate(oldLead, savedLead, employee,
                    "Consolidated lead update by employee: " + employee.getEmail());

            // Send email notification
            String role = employee.getEmail().equals("redcircle0908@gmail.com") ? "ADMIN" : "EMPLOYEE";
            emailService.sendLeadChangeNotificationToAdmin(oldLead, savedLead,
                    employee.getFirstName() + " " + employee.getLastName(),
                    role, changes);
        }

        log.info("Lead {} updated successfully with {} changes", id, changes.size());
        return mapToResponseDTO(savedLead);
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
        }

        return contactInfo.toString();
    }

    private List<String> processStatisticsUpdates(Lead lead, EmployeeLeadUpdateDTO updateDTO) {
        List<String> statsChanges = new ArrayList<>();

        if (Boolean.TRUE.equals(updateDTO.getCallsMade())) {
            lead.setCallsMadeCount(lead.getCallsMadeCount() + 1);
            statsChanges.add("Phone call made (Total: " + lead.getCallsMadeCount() + ")");
        }

        if (Boolean.TRUE.equals(updateDTO.getMeetingBooked())) {
            lead.setMeetingsBookedCount(lead.getMeetingsBookedCount() + 1);
            statsChanges.add("Meeting booked (Total: " + lead.getMeetingsBookedCount() + ")");
        }

        if (Boolean.TRUE.equals(updateDTO.getMeetingDone())) {
            lead.setMeetingsDoneCount(lead.getMeetingsDoneCount() + 1);
            statsChanges.add("Meeting completed (Total: " + lead.getMeetingsDoneCount() + ")");
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
            lead.setNextFollowUp(updateDTO.getNextFollowUpDescription());
            followUpChange.append("Follow-up description: \"").append(updateDTO.getNextFollowUpDescription()).append("\". ");
        }

        return followUpChange.toString();
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

    private Lead mapToEntity(LeadDTO dto, Employee assignedEmployee) {
        return Lead.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .leadType(dto.getLeadType())
                .leadStage(dto.getLeadStage())
                .nextFollowUpDate(dto.getNextFollowUpDate())
                .remarks(dto.getRemarks())
                .nextFollowUp(dto.getNextFollowUp())
                .assignedEmployee(assignedEmployee)
                .source(dto.getSource())
                .isActive(true)
                .interestedService(dto.getInterestedService())
                .serviceSubcategory(dto.getServiceSubcategory())
                .serviceSubSubcategory(dto.getServiceSubSubcategory())
                .serviceDescription(dto.getServiceDescription())
                .updateCount(0)
                .lastUpdatedBy("SYSTEM")
                .callsMadeCount(0)
                .meetingsBookedCount(0)
                .meetingsDoneCount(0)
                .build();
    }

    private LeadResponseDTO mapToResponseDTO(Lead lead) {
        LeadResponseDTO dto = LeadResponseDTO.builder()
                .id(lead.getId())
                .name(lead.getName())
                .email(lead.getEmail())
                .phoneNumber(lead.getPhoneNumber())
                .leadType(lead.getLeadType())
                .leadStage(lead.getLeadStage())
                .nextFollowUpDate(lead.getNextFollowUpDate())
                .remarks(lead.getRemarks())
                .nextFollowUp(lead.getNextFollowUp())
                .source(lead.getSource())
                .isActive(lead.getIsActive())
                .callsMadeCount(lead.getCallsMadeCount())
                .meetingsBookedCount(lead.getMeetingsBookedCount())
                .meetingsDoneCount(lead.getMeetingsDoneCount())
                .interestedService(lead.getInterestedService())
                .serviceSubcategory(lead.getServiceSubcategory())
                .serviceSubSubcategory(lead.getServiceSubSubcategory())
                .serviceDescription(lead.getServiceDescription())
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .updateCount(lead.getUpdateCount())
                .lastUpdatedBy(lead.getLastUpdatedBy())
                .lastContactDate(lead.getLastContactDate())
                .build();

        if (lead.getAssignedEmployee() != null) {
            EmployeeResponseDTO empDto = EmployeeResponseDTO.builder()
                    .id(lead.getAssignedEmployee().getId())
                    .firstName(lead.getAssignedEmployee().getFirstName())
                    .lastName(lead.getAssignedEmployee().getLastName())
                    .email(lead.getAssignedEmployee().getEmail())
                    .employeeCode(lead.getAssignedEmployee().getEmployeeCode())
                    .department(lead.getAssignedEmployee().getDepartment())
                    .position(lead.getAssignedEmployee().getPosition())
                    .build();
            dto.setAssignedEmployee(empDto);
        }

        return dto;
    }
}