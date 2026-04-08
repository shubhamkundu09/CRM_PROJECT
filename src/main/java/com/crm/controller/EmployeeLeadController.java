package com.crm.controller;

import com.crm.dto.ApiResponse;
import com.crm.dto.EmployeeLeadUpdateDTO;
import com.crm.dto.LeadResponseDTO;
import com.crm.service.EmployeeLeadService;
import com.crm.util.CryptoUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee/leads")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
@Slf4j
public class EmployeeLeadController {

    private final EmployeeLeadService employeeLeadService;

    @PutMapping("/{leadId}/update")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> updateLead(
            @PathVariable String leadId,
            @Valid @RequestBody EmployeeLeadUpdateDTO updateDTO,
            Authentication authentication,
            HttpServletRequest request) {
        Long decryptedLeadId = CryptoUtil.decryptToLong(leadId);
        log.info("Employee updating lead - Lead ID: {}", decryptedLeadId);
        String email = authentication.getName();
        LeadResponseDTO updatedLead = employeeLeadService.updateLead(decryptedLeadId, updateDTO, email);
        return ResponseEntity.ok(ApiResponse.success(updatedLead, "Lead updated successfully", request.getRequestURI()));
    }
}