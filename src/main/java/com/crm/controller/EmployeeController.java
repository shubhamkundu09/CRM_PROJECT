package com.crm.controller;

import com.crm.dto.*;
import com.crm.entity.LeadStage;
import com.crm.entity.LeadType;
import com.crm.service.*;
import com.crm.util.CryptoUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
@Slf4j
public class EmployeeController {

    private final EmployeeProfileService employeeProfileService;
    private final LeadService leadService;
    private final EmployeeLeadService employeeLeadService;
    private final LeadHistoryService leadHistoryService;
    private final ContactService contactService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<EmployeeProfileDTO>> getMyProfile(
            Authentication authentication,
            HttpServletRequest request) {
        log.info("Employee fetching own profile");
        String email = authentication.getName();
        EmployeeProfileDTO profile = employeeProfileService.getEmployeeProfileByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved successfully", request.getRequestURI()));
    }

    // ==================== LEAD MANAGEMENT WITH PAGINATION & FILTERING ====================

    @GetMapping("/my-leads")
    public ResponseEntity<ApiResponse<Page<LeadResponseDTO>>> getMyLeads(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String leadEmail,  // CHANGED: was 'email' to avoid conflict
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) LeadType leadType,
            @RequestParam(required = false) LeadStage leadStage,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextFollowUpDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate followUpFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate followUpTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(required = false) Integer minCallsMade,
            @RequestParam(required = false) Integer maxCallsMade,
            @RequestParam(required = false) Integer minMeetingsBooked,
            @RequestParam(required = false) Integer maxMeetingsBooked,
            @RequestParam(required = false) Integer minMeetingsDone,
            @RequestParam(required = false) Integer maxMeetingsDone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nextFollowUpDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Employee fetching assigned leads with pagination - page: {}, size: {}", page, size);

        String userEmail = authentication.getName();  // CHANGED: was 'email'
        EmployeeProfileDTO profile = employeeProfileService.getEmployeeProfileByEmail(userEmail);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LeadResponseDTO> leads = leadService.searchLeads(
                name, leadEmail, phone, leadType, leadStage, profile.getId(),
                isActive != null ? isActive : true, source,
                nextFollowUpDate, followUpFrom, followUpTo, createdFrom, createdTo,
                null, null,  // updatedFrom, updatedTo
                minCallsMade, maxCallsMade, minMeetingsBooked, maxMeetingsBooked,
                minMeetingsDone, maxMeetingsDone, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(leads, "Your leads retrieved successfully", request.getRequestURI()));
    }

    // ==================== CONTACT LEADS VIEW ====================

    @GetMapping("/contacts/{contactId}/leads")
    public ResponseEntity<ApiResponse<List<LeadSummaryDTO>>> getContactLeads(
            @PathVariable Long contactId,
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Employee fetching leads for contact ID: {}", contactId);

        // Verify employee has access to at least one lead of this contact
        String userEmail = authentication.getName();  // CHANGED: was 'email'
        EmployeeProfileDTO profile = employeeProfileService.getEmployeeProfileByEmail(userEmail);

        List<LeadSummaryDTO> contactLeads = contactService.getContactLeads(contactId);

        // Filter leads to only those assigned to this employee
        List<LeadSummaryDTO> accessibleLeads = contactLeads.stream()
                .filter(lead -> lead.getAssignedEmployeeName() != null &&
                        lead.getAssignedEmployeeName().toLowerCase().contains(profile.getFirstName().toLowerCase()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(accessibleLeads, "Contact leads retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/contacts/{contactId}")
    public ResponseEntity<ApiResponse<ContactDetailDTO>> getContactDetails(
            @PathVariable Long contactId,
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Employee fetching contact details for ID: {}", contactId);

        // Verify employee has access to this contact through their leads
        String userEmail = authentication.getName();  // CHANGED: was 'email'
        EmployeeProfileDTO profile = employeeProfileService.getEmployeeProfileByEmail(userEmail);

        ContactDetailDTO contact = contactService.getContactWithLeads(contactId);

        // Filter leads to only those assigned to this employee
        List<LeadSummaryDTO> accessibleLeads = contact.getLeads().stream()
                .filter(lead -> lead.getAssignedEmployeeName() != null &&
                        lead.getAssignedEmployeeName().toLowerCase().contains(profile.getFirstName().toLowerCase()))
                .toList();
        contact.setLeads(accessibleLeads);

        return ResponseEntity.ok(ApiResponse.success(contact, "Contact details retrieved successfully", request.getRequestURI()));
    }

    // ==================== LEAD STATISTICS ====================

    @GetMapping("/my-leads/statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getMyLeadStatistics(
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Employee fetching lead statistics");

        String userEmail = authentication.getName();  // CHANGED: was 'email'
        EmployeeProfileDTO profile = employeeProfileService.getEmployeeProfileByEmail(userEmail);

        List<LeadResponseDTO> myLeads = leadService.getLeadsByEmployee(profile.getId());

        Map<String, Long> statistics = Map.of(
                "total_leads", (long) myLeads.size(),
                "active_leads", myLeads.stream().filter(LeadResponseDTO::getIsActive).count(),
                "hot_leads", myLeads.stream().filter(l -> l.getLeadType() == LeadType.HOT).count(),
                "warm_leads", myLeads.stream().filter(l -> l.getLeadType() == LeadType.WARM).count(),
                "cold_leads", myLeads.stream().filter(l -> l.getLeadType() == LeadType.COLD).count(),
                "interested_leads", myLeads.stream().filter(l -> l.getLeadStage() == LeadStage.INTERESTED).count(),
                "not_interested_leads", myLeads.stream().filter(l -> l.getLeadStage() == LeadStage.NOT_INTERESTED).count(),
                "normal_leads", myLeads.stream().filter(l -> l.getLeadStage() == LeadStage.NORMAL).count()
        );

        return ResponseEntity.ok(ApiResponse.success(statistics, "Lead statistics retrieved successfully", request.getRequestURI()));
    }

    // ==================== LEAD HISTORY ====================

    @GetMapping("/my-leads/{leadId}/history")
    public ResponseEntity<ApiResponse<List<LeadHistoryDTO>>> getMyLeadHistory(
            @PathVariable String leadId,
            Authentication authentication,
            HttpServletRequest request) {

        Long decryptedLeadId = CryptoUtil.decryptToLong(leadId);
        log.info("Employee fetching lead history for ID: {}", decryptedLeadId);

        String userEmail = authentication.getName();  // CHANGED: was 'email'
        EmployeeProfileDTO profile = employeeProfileService.getEmployeeProfileByEmail(userEmail);

        LeadResponseDTO lead = leadService.getLeadById(decryptedLeadId);
        if (!lead.getAssignedEmployee().getId().equals(profile.getId())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You don't have access to this lead", 403, request.getRequestURI()));
        }

        List<LeadHistoryDTO> history = leadHistoryService.getLeadHistory(decryptedLeadId);
        return ResponseEntity.ok(ApiResponse.success(history, "Lead history retrieved successfully", request.getRequestURI()));
    }

    // ==================== FOLLOW-UP MANAGEMENT ====================

    @GetMapping("/my-leads/followups/today")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getTodayFollowUps(
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Employee fetching today's follow-ups");

        String userEmail = authentication.getName();  // CHANGED: was 'email'
        EmployeeProfileDTO profile = employeeProfileService.getEmployeeProfileByEmail(userEmail);

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<LeadResponseDTO> leads = leadService.searchLeads(
                null, null, null, null, null, profile.getId(), true, null,
                LocalDate.now(), null, null, null, null, null, null,
                null, null, null, null, null, null, pageable
        );

        List<LeadResponseDTO> todayFollowUps = leads.getContent().stream()
                .filter(lead -> lead.getNextFollowUpDate() != null &&
                        lead.getNextFollowUpDate().equals(LocalDate.now()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(todayFollowUps, "Today's follow-ups retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/my-leads/followups/upcoming")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getUpcomingFollowUps(
            @RequestParam(defaultValue = "7") int days,
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Employee fetching upcoming follow-ups for next {} days", days);

        String userEmail = authentication.getName();  // CHANGED: was 'email'
        EmployeeProfileDTO profile = employeeProfileService.getEmployeeProfileByEmail(userEmail);

        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(days);

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("nextFollowUpDate").ascending());
        Page<LeadResponseDTO> leads = leadService.searchLeads(
                null, null, null, null, null, profile.getId(), true, null,
                null, today, futureDate, null, null, null, null,
                null, null, null, null, null, null, pageable
        );

        List<LeadResponseDTO> upcomingFollowUps = leads.getContent().stream()
                .filter(lead -> lead.getNextFollowUpDate() != null &&
                        lead.getNextFollowUpDate().isAfter(today))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(upcomingFollowUps, "Upcoming follow-ups retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/my-leads/followups/pending")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getPendingFollowUps(
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Employee fetching pending/overdue follow-ups");

        String userEmail = authentication.getName();  // CHANGED: was 'email'
        EmployeeProfileDTO profile = employeeProfileService.getEmployeeProfileByEmail(userEmail);

        LocalDate today = LocalDate.now();

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("nextFollowUpDate").ascending());
        Page<LeadResponseDTO> leads = leadService.searchLeads(
                null, null, null, null, null, profile.getId(), true, null,
                null, null, today.minusDays(365), null, null, null, null,
                null, null, null, null, null, null, pageable
        );

        List<LeadResponseDTO> pendingFollowUps = leads.getContent().stream()
                .filter(lead -> lead.getNextFollowUpDate() != null &&
                        lead.getNextFollowUpDate().isBefore(today))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(pendingFollowUps, "Pending/overdue follow-ups retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/my-leads/followups/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFollowUpStatistics(
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Employee fetching follow-up statistics");

        String userEmail = authentication.getName();  // CHANGED: was 'email'
        EmployeeProfileDTO profile = employeeProfileService.getEmployeeProfileByEmail(userEmail);

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<LeadResponseDTO> leads = leadService.searchLeads(
                null, null, null, null, null, profile.getId(), true, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, pageable
        );

        LocalDate today = LocalDate.now();

        long todayCount = leads.getContent().stream()
                .filter(l -> l.getNextFollowUpDate() != null && l.getNextFollowUpDate().equals(today))
                .count();

        long upcomingCount = leads.getContent().stream()
                .filter(l -> l.getNextFollowUpDate() != null && l.getNextFollowUpDate().isAfter(today))
                .count();

        long pendingCount = leads.getContent().stream()
                .filter(l -> l.getNextFollowUpDate() != null && l.getNextFollowUpDate().isBefore(today))
                .count();

        long noFollowUpCount = leads.getContent().stream()
                .filter(l -> l.getNextFollowUpDate() == null)
                .count();

        Map<String, Object> statistics = Map.of(
                "today_followups", todayCount,
                "upcoming_followups", upcomingCount,
                "pending_followups", pendingCount,
                "no_followup_set", noFollowUpCount,
                "total_leads", (long) leads.getContent().size()
        );

        return ResponseEntity.ok(ApiResponse.success(statistics, "Follow-up statistics retrieved successfully", request.getRequestURI()));
    }

    // ==================== SINGLE LEAD OPERATIONS ====================

    @GetMapping("/my-leads/{leadId}")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> getMyLeadById(
            @PathVariable String leadId,
            Authentication authentication,
            HttpServletRequest request) {

        Long decryptedLeadId = CryptoUtil.decryptToLong(leadId);
        log.info("Employee fetching lead details for ID: {}", decryptedLeadId);

        LeadResponseDTO lead = leadService.getLeadById(decryptedLeadId);

        String userEmail = authentication.getName();  // CHANGED: was 'email'
        EmployeeProfileDTO profile = employeeProfileService.getEmployeeProfileByEmail(userEmail);

        if (!lead.getAssignedEmployee().getId().equals(profile.getId())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("You don't have access to this lead", 403, request.getRequestURI()));
        }

        return ResponseEntity.ok(ApiResponse.success(lead, "Lead details retrieved successfully", request.getRequestURI()));
    }

    @PutMapping("/my-leads/{leadId}/update")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> updateMyLead(
            @PathVariable String leadId,
            @Valid @RequestBody EmployeeLeadUpdateDTO updateDTO,
            Authentication authentication,
            HttpServletRequest request) {

        Long decryptedLeadId = CryptoUtil.decryptToLong(leadId);
        log.info("Employee updating lead - Lead ID: {}", decryptedLeadId);

        String userEmail = authentication.getName();  // CHANGED: was 'email'
        LeadResponseDTO updatedLead = employeeLeadService.updateLead(decryptedLeadId, updateDTO, userEmail);

        return ResponseEntity.ok(ApiResponse.success(updatedLead, "Lead updated successfully", request.getRequestURI()));
    }
}