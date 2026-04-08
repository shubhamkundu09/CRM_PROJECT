package com.crm.controller;

import com.crm.dto.*;
import com.crm.entity.LeadStage;
import com.crm.entity.LeadType;
import com.crm.service.LeadHistoryService;
import com.crm.service.LeadService;
import com.crm.util.CryptoUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/leads")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class LeadController {

    private final LeadService leadService;
    private final LeadHistoryService leadHistoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<LeadResponseDTO>> createLead(
            @Valid @RequestBody LeadDTO leadDTO,
            HttpServletRequest request) {
        log.info("Creating new lead with email: {}", leadDTO.getEmail());
        LeadResponseDTO createdLead = leadService.createLead(leadDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdLead, "Lead created successfully", request.getRequestURI()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> updateLead(
            @PathVariable String id,
            @Valid @RequestBody LeadUpdateDTO leadUpdateDTO,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Updating lead with ID: {}", decryptedId);
        LeadResponseDTO updatedLead = leadService.updateLead(decryptedId, leadUpdateDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedLead, "Lead updated successfully", request.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLead(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Deleting lead with ID: {}", decryptedId);
        leadService.deleteLead(decryptedId);
        return ResponseEntity.ok(ApiResponse.success("Lead deleted successfully", request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> getLeadById(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Fetching lead with ID: {}", decryptedId);
        LeadResponseDTO lead = leadService.getLeadById(decryptedId);
        return ResponseEntity.ok(ApiResponse.success(lead, "Lead retrieved successfully", request.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getAllLeads(HttpServletRequest request) {
        log.info("Fetching all leads");
        List<LeadResponseDTO> leads = leadService.getAllLeads();
        return ResponseEntity.ok(ApiResponse.success(leads, "Leads retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getLeadsByEmployee(
            @PathVariable String employeeId,
            HttpServletRequest request) {
        Long decryptedEmployeeId = CryptoUtil.decryptToLong(employeeId);
        log.info("Fetching leads for employee ID: {}", decryptedEmployeeId);
        List<LeadResponseDTO> leads = leadService.getLeadsByEmployee(decryptedEmployeeId);
        return ResponseEntity.ok(ApiResponse.success(leads, "Leads retrieved successfully for employee", request.getRequestURI()));
    }

    @GetMapping("/type/{leadType}")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getLeadsByType(
            @PathVariable LeadType leadType,
            HttpServletRequest request) {
        log.info("Fetching leads by type: {}", leadType);
        List<LeadResponseDTO> leads = leadService.getLeadsByType(leadType);
        return ResponseEntity.ok(ApiResponse.success(leads, "Leads retrieved successfully by type", request.getRequestURI()));
    }

    @GetMapping("/stage/{leadStage}")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getLeadsByStage(
            @PathVariable LeadStage leadStage,
            HttpServletRequest request) {
        log.info("Fetching leads by stage: {}", leadStage);
        List<LeadResponseDTO> leads = leadService.getLeadsByStage(leadStage);
        return ResponseEntity.ok(ApiResponse.success(leads, "Leads retrieved successfully by stage", request.getRequestURI()));
    }

    @GetMapping("/followups/today")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getTodayFollowUps(HttpServletRequest request) {
        log.info("Fetching today's follow-ups");
        List<LeadResponseDTO> leads = leadService.getTodayFollowUps();
        return ResponseEntity.ok(ApiResponse.success(leads, "Today's follow-ups retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/followups/pending")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getPendingFollowUps(HttpServletRequest request) {
        log.info("Fetching pending follow-ups");
        List<LeadResponseDTO> leads = leadService.getPendingFollowUps();
        return ResponseEntity.ok(ApiResponse.success(leads, "Pending follow-ups retrieved successfully", request.getRequestURI()));
    }

    @PatchMapping("/{id}/statistics")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> updateLeadStatistics(
            @PathVariable String id,
            @RequestBody LeadStatisticsUpdateDTO statisticsDTO,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Updating statistics for lead ID: {}", decryptedId);
        LeadResponseDTO updatedLead = leadService.updateLeadStatistics(decryptedId, statisticsDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedLead, "Lead statistics updated successfully", request.getRequestURI()));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLeadStatistics(HttpServletRequest request) {
        log.info("Fetching lead statistics");
        Map<String, Long> statistics = leadService.getLeadStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics, "Lead statistics retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getLeadsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            HttpServletRequest request) {
        log.info("Fetching leads between {} and {}", startDate, endDate);
        List<LeadResponseDTO> leads = leadService.getLeadsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(leads, "Leads retrieved successfully for date range", request.getRequestURI()));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<LeadHistoryDTO>>> getLeadHistory(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Fetching history for lead ID: {}", decryptedId);
        List<LeadHistoryDTO> history = leadHistoryService.getLeadHistory(decryptedId);
        return ResponseEntity.ok(ApiResponse.success(history, "Lead history retrieved successfully", request.getRequestURI()));
    }
}