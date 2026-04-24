package com.crm.controller;

import com.crm.dto.*;
import com.crm.entity.Employee;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final EmployeeService employeeService;
    private final LeadService leadService;
    private final LeadHistoryService leadHistoryService;
    private final WebsiteLeadService websiteLeadService;
    private final YouTubeVideoService youTubeVideoService;
    private final PartnerInquiryService partnerInquiryService;

    // ==================== EMPLOYEE MANAGEMENT ====================

    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> createEmployee(
            @Valid @RequestBody EmployeeDTO employeeDTO,
            HttpServletRequest request) {
        log.info("Creating new employee with email: {}", employeeDTO.getEmail());
        EmployeeResponseDTO createdEmployee = employeeService.createEmployee(employeeDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdEmployee, "Employee created successfully", request.getRequestURI()));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> updateEmployee(
            @PathVariable String id,
            @Valid @RequestBody EmployeeDTO employeeDTO,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Updating employee with ID: {}", decryptedId);
        EmployeeResponseDTO updatedEmployee = employeeService.updateEmployee(decryptedId, employeeDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedEmployee, "Employee updated successfully", request.getRequestURI()));
    }

    @DeleteMapping("/employees/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateEmployee(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Deactivating employee with ID: {}", decryptedId);
        employeeService.deactivateEmployee(decryptedId);
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated successfully. Employee can no longer login.", request.getRequestURI()));
    }

    @DeleteMapping("/employees/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteEmployee(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Permanently deleting employee with ID: {}", decryptedId);
        employeeService.permanentlyDeleteEmployee(decryptedId);
        return ResponseEntity.ok(ApiResponse.success("Employee permanently deleted from system.", request.getRequestURI()));
    }

    @PatchMapping("/employees/{id}/reactivate")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> reactivateEmployee(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Reactivating employee with ID: {}", decryptedId);
        EmployeeResponseDTO reactivatedEmployee = employeeService.reactivateEmployee(decryptedId);
        return ResponseEntity.ok(ApiResponse.success(reactivatedEmployee, "Employee reactivated successfully", request.getRequestURI()));
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Soft deleting employee with ID: {}", decryptedId);
        employeeService.deactivateEmployee(decryptedId);
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated successfully", request.getRequestURI()));
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getEmployeeById(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Fetching employee with ID: {}", decryptedId);
        EmployeeResponseDTO employee = employeeService.getEmployeeById(decryptedId);
        return ResponseEntity.ok(ApiResponse.success(employee, "Employee retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getAllEmployees(HttpServletRequest request) {
        log.info("Fetching all employees");
        List<EmployeeResponseDTO> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(ApiResponse.success(employees, "Employees retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/employees/active")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getActiveEmployees(HttpServletRequest request) {
        log.info("Fetching active employees");
        List<EmployeeResponseDTO> employees = employeeService.getActiveEmployees();
        return ResponseEntity.ok(ApiResponse.success(employees, "Active employees retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/employees/deactivated")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getDeactivatedEmployees(HttpServletRequest request) {
        log.info("Fetching deactivated employees");
        List<EmployeeResponseDTO> employees = employeeService.getDeactivatedEmployees();
        return ResponseEntity.ok(ApiResponse.success(employees, "Deactivated employees retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/employees/department/{department}")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getEmployeesByDepartment(
            @PathVariable String department,
            HttpServletRequest request) {
        log.info("Fetching employees from department: {}", department);
        List<EmployeeResponseDTO> employees = employeeService.getEmployeesByDepartment(department);
        return ResponseEntity.ok(ApiResponse.success(employees, "Employees retrieved successfully for department: " + department, request.getRequestURI()));
    }

    @GetMapping("/employees/search")
    public ResponseEntity<Page<Employee>> searchEmployees(
            @RequestParam(required = false) String empCode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "joiningDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Employee> result = employeeService.searchEmployees(empCode, name, department, isActive, pageable);
        return ResponseEntity.ok(result);
    }

    // ==================== LEAD MANAGEMENT ====================

    @PostMapping("/leads")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> createLead(
            @Valid @RequestBody LeadDTO leadDTO,
            HttpServletRequest request) {
        log.info("Creating new lead with email: {}", leadDTO.getEmail());
        LeadResponseDTO createdLead = leadService.createLead(leadDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdLead, "Lead created successfully", request.getRequestURI()));
    }

    @PutMapping("/leads/{id}")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> updateLead(
            @PathVariable String id,
            @Valid @RequestBody LeadUpdateDTO leadUpdateDTO,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Updating lead with ID: {}", decryptedId);
        LeadResponseDTO updatedLead = leadService.updateLead(decryptedId, leadUpdateDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedLead, "Lead updated successfully", request.getRequestURI()));
    }

    @DeleteMapping("/leads/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLead(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Deleting lead with ID: {}", decryptedId);
        leadService.deleteLead(decryptedId);
        return ResponseEntity.ok(ApiResponse.success("Lead deleted successfully", request.getRequestURI()));
    }

    @GetMapping("/leads/{id}")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> getLeadById(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Fetching lead with ID: {}", decryptedId);
        LeadResponseDTO lead = leadService.getLeadById(decryptedId);
        return ResponseEntity.ok(ApiResponse.success(lead, "Lead retrieved successfully", request.getRequestURI()));
    }

    @PatchMapping("/leads/{id}/statistics")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> updateLeadStatistics(
            @PathVariable String id,
            @RequestBody LeadStatisticsUpdateDTO statisticsDTO,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Updating statistics for lead ID: {}", decryptedId);
        LeadResponseDTO updatedLead = leadService.updateLeadStatistics(decryptedId, statisticsDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedLead, "Lead statistics updated successfully", request.getRequestURI()));
    }

    @GetMapping("/leads/statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLeadStatistics(HttpServletRequest request) {
        log.info("Fetching lead statistics");
        Map<String, Long> statistics = leadService.getLeadStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics, "Lead statistics retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/leads/{id}/history")
    public ResponseEntity<ApiResponse<List<LeadHistoryDTO>>> getLeadHistory(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Fetching history for lead ID: {}", decryptedId);
        List<LeadHistoryDTO> history = leadHistoryService.getLeadHistory(decryptedId);
        return ResponseEntity.ok(ApiResponse.success(history, "Lead history retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/leads/search")
    public ResponseEntity<ApiResponse<Page<LeadResponseDTO>>> searchLeads(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) LeadType leadType,
            @RequestParam(required = false) LeadStage leadStage,
            @RequestParam(required = false) Long assignedEmployeeId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) LocalDate nextFollowUpDate,
            @RequestParam(required = false) LocalDate followUpFrom,
            @RequestParam(required = false) LocalDate followUpTo,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo,
            @RequestParam(required = false) LocalDate updatedFrom,
            @RequestParam(required = false) LocalDate updatedTo,
            @RequestParam(required = false) Integer minCallsMade,
            @RequestParam(required = false) Integer maxCallsMade,
            @RequestParam(required = false) Integer minMeetingsBooked,
            @RequestParam(required = false) Integer maxMeetingsBooked,
            @RequestParam(required = false) Integer minMeetingsDone,
            @RequestParam(required = false) Integer maxMeetingsDone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {

        log.info("Searching leads with pagination - page: {}, size: {}, sortBy: {}, sortDir: {}",
                page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LeadResponseDTO> leads = leadService.searchLeads(
                name, email, phone, leadType, leadStage, assignedEmployeeId, isActive, source,
                nextFollowUpDate, followUpFrom, followUpTo, createdFrom, createdTo, updatedFrom, updatedTo,
                minCallsMade, maxCallsMade, minMeetingsBooked, maxMeetingsBooked, minMeetingsDone, maxMeetingsDone,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(leads, "Leads search completed", request.getRequestURI()));
    }

    // ==================== WEBSITE LEAD MANAGEMENT ====================

    @GetMapping("/website-leads")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getWebsiteLeads(HttpServletRequest request) {
        log.info("Fetching all website leads");
        List<LeadResponseDTO> websiteLeads = websiteLeadService.getWebsiteLeads();
        return ResponseEntity.ok(ApiResponse.success(websiteLeads, "Website leads retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/website-leads/{id}")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> getWebsiteLeadById(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Fetching website lead with ID: {}", decryptedId);
        LeadResponseDTO lead = websiteLeadService.getWebsiteLeadById(decryptedId);
        return ResponseEntity.ok(ApiResponse.success(lead, "Website lead retrieved successfully", request.getRequestURI()));
    }

    @PutMapping("/website-leads/{id}")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> updateWebsiteLead(
            @PathVariable String id,
            @RequestBody WebsiteLeadUpdateDTO updateDTO,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Admin updating website lead with ID: {}", decryptedId);
        LeadResponseDTO updatedLead = websiteLeadService.updateWebsiteLead(decryptedId, updateDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedLead, "Website lead updated successfully", request.getRequestURI()));
    }

    @DeleteMapping("/website-leads/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWebsiteLead(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Deleting (soft delete) website lead with ID: {}", decryptedId);
        websiteLeadService.deleteWebsiteLead(decryptedId);
        return ResponseEntity.ok(ApiResponse.success("Website lead deleted successfully", request.getRequestURI()));
    }

    @GetMapping("/website-leads/statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getWebsiteLeadStatistics(HttpServletRequest request) {
        log.info("Fetching website lead statistics");
        Map<String, Long> statistics = websiteLeadService.getWebsiteLeadStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics, "Website lead statistics retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/website-leads/followups/today")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getTodayWebsiteFollowUps(HttpServletRequest request) {
        log.info("Fetching today's follow-ups for website leads");
        List<LeadResponseDTO> todayFollowUps = websiteLeadService.getTodayWebsiteFollowUps();
        return ResponseEntity.ok(ApiResponse.success(todayFollowUps, "Today's website lead follow-ups retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/website-leads/followups/pending")
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getPendingWebsiteFollowUps(HttpServletRequest request) {
        log.info("Fetching pending follow-ups for website leads");
        List<LeadResponseDTO> pendingFollowUps = websiteLeadService.getPendingWebsiteFollowUps();
        return ResponseEntity.ok(ApiResponse.success(pendingFollowUps, "Pending website lead follow-ups retrieved successfully", request.getRequestURI()));
    }

    @PatchMapping("/website-leads/{id}/assign")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> assignWebsiteLead(
            @PathVariable String id,
            @RequestParam Long employeeId,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Assigning website lead {} to employee {}", decryptedId, employeeId);
        LeadResponseDTO lead = websiteLeadService.assignWebsiteLead(decryptedId, employeeId);
        return ResponseEntity.ok(ApiResponse.success(lead, "Website lead assigned successfully", request.getRequestURI()));
    }





//    ======================= youtube videos api for website =========================

    // ==================== YOUTUBE VIDEO MANAGEMENT ====================

    @PostMapping("/youtube-videos")
    public ResponseEntity<ApiResponse<YouTubeVideoDTO>> createYouTubeVideo(
            @Valid @RequestBody YouTubeVideoCreateDTO createDTO,
            HttpServletRequest request) {
        log.info("Admin creating new YouTube video with title: {}", createDTO.getTitle());
        YouTubeVideoDTO createdVideo = youTubeVideoService.createVideo(createDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdVideo, "YouTube video created successfully", request.getRequestURI()));
    }

    @PutMapping("/youtube-videos/{id}")
    public ResponseEntity<ApiResponse<YouTubeVideoDTO>> updateYouTubeVideo(
            @PathVariable Long id,
            @Valid @RequestBody YouTubeVideoUpdateDTO updateDTO,
            HttpServletRequest request) {
        updateDTO.setId(id);
        log.info("Admin updating YouTube video with ID: {}", id);
        YouTubeVideoDTO updatedVideo = youTubeVideoService.updateVideo(updateDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedVideo, "YouTube video updated successfully", request.getRequestURI()));
    }

    @DeleteMapping("/youtube-videos/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteYouTubeVideo(
            @PathVariable Long id,
            HttpServletRequest request) {
        log.info("Admin deleting YouTube video with ID: {}", id);
        youTubeVideoService.deleteVideo(id);
        return ResponseEntity.ok(ApiResponse.success("YouTube video deleted successfully", request.getRequestURI()));
    }

    @GetMapping("/youtube-videos/{id}")
    public ResponseEntity<ApiResponse<YouTubeVideoDTO>> getYouTubeVideoById(
            @PathVariable Long id,
            HttpServletRequest request) {
        log.info("Admin fetching YouTube video with ID: {}", id);
        YouTubeVideoDTO video = youTubeVideoService.getVideoById(id);
        return ResponseEntity.ok(ApiResponse.success(video, "YouTube video retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/youtube-videos")
    public ResponseEntity<ApiResponse<Page<YouTubeVideoDTO>>> getAllYouTubeVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {
        log.info("Admin fetching all YouTube videos - page: {}, size: {}", page, size);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<YouTubeVideoDTO> videos = youTubeVideoService.getAllVideos(pageable);
        return ResponseEntity.ok(ApiResponse.success(videos, "YouTube videos retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/youtube-videos/all")
    public ResponseEntity<ApiResponse<List<YouTubeVideoDTO>>> getAllYouTubeVideosList(HttpServletRequest request) {
        log.info("Admin fetching all YouTube videos as list");
        List<YouTubeVideoDTO> videos = youTubeVideoService.getAllVideosList();
        return ResponseEntity.ok(ApiResponse.success(videos, "YouTube videos retrieved successfully", request.getRequestURI()));
    }

    @PatchMapping("/youtube-videos/{id}/toggle-status")
    public ResponseEntity<ApiResponse<YouTubeVideoDTO>> toggleYouTubeVideoStatus(
            @PathVariable Long id,
            HttpServletRequest request) {
        log.info("Admin toggling status for YouTube video with ID: {}", id);
        YouTubeVideoDTO video = youTubeVideoService.toggleVideoStatus(id);
        return ResponseEntity.ok(ApiResponse.success(video, "YouTube video status updated successfully", request.getRequestURI()));
    }

    @PatchMapping("/youtube-videos/{id}/order")
    public ResponseEntity<ApiResponse<YouTubeVideoDTO>> updateYouTubeVideoOrder(
            @PathVariable Long id,
            @RequestParam Integer displayOrder,
            HttpServletRequest request) {
        log.info("Admin updating display order for YouTube video ID: {} to {}", id, displayOrder);
        YouTubeVideoDTO video = youTubeVideoService.updateVideoOrder(id, displayOrder);
        return ResponseEntity.ok(ApiResponse.success(video, "YouTube video order updated successfully", request.getRequestURI()));
    }






//    ====================== partner inquries ==================================

    @GetMapping("/partner-inquiries")
    public ResponseEntity<ApiResponse<Page<PartnerInquiryResponseDTO>>> getAllPartnerInquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {

        log.info("Admin fetching all partner inquiries - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PartnerInquiryResponseDTO> inquiries = partnerInquiryService.getAllInquiries(pageable);
        return ResponseEntity.ok(ApiResponse.success(inquiries, "Partner inquiries retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/partner-inquiries/search")
    public ResponseEntity<ApiResponse<Page<PartnerInquiryResponseDTO>>> searchPartnerInquiries(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Boolean isProcessed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {

        log.info("Searching partner inquiries - name: {}, email: {}, phone: {}, processed: {}",
                name, email, phone, isProcessed);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PartnerInquiryResponseDTO> inquiries = partnerInquiryService.searchInquiries(name, email, phone, isProcessed, pageable);
        return ResponseEntity.ok(ApiResponse.success(inquiries, "Partner inquiries search completed", request.getRequestURI()));
    }

    @GetMapping("/partner-inquiries/{id}")
    public ResponseEntity<ApiResponse<PartnerInquiryResponseDTO>> getPartnerInquiryById(
            @PathVariable String id,
            HttpServletRequest request) {

        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Admin fetching partner inquiry with ID: {}", decryptedId);

        PartnerInquiryResponseDTO inquiry = partnerInquiryService.getInquiryById(decryptedId);
        return ResponseEntity.ok(ApiResponse.success(inquiry, "Partner inquiry retrieved successfully", request.getRequestURI()));
    }

    @PatchMapping("/partner-inquiries/{id}/status")
    public ResponseEntity<ApiResponse<PartnerInquiryResponseDTO>> updatePartnerInquiryStatus(
            @PathVariable String id,
            @RequestBody PartnerInquiryUpdateDTO updateDTO,
            HttpServletRequest request) {

        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Admin updating partner inquiry status for ID: {} to processed: {}", decryptedId, updateDTO.getIsProcessed());

        PartnerInquiryResponseDTO inquiry = partnerInquiryService.updateInquiryStatus(decryptedId, updateDTO);
        return ResponseEntity.ok(ApiResponse.success(inquiry, "Partner inquiry status updated successfully", request.getRequestURI()));
    }

    @DeleteMapping("/partner-inquiries/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePartnerInquiry(
            @PathVariable String id,
            HttpServletRequest request) {

        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Admin deleting partner inquiry with ID: {}", decryptedId);

        partnerInquiryService.deleteInquiry(decryptedId);
        return ResponseEntity.ok(ApiResponse.success("Partner inquiry deleted successfully", request.getRequestURI()));
    }

    @GetMapping("/partner-inquiries/statistics/unprocessed-count")
    public ResponseEntity<ApiResponse<Long>> getUnprocessedInquiriesCount(HttpServletRequest request) {
        log.info("Admin fetching unprocessed partner inquiries count");
        long count = partnerInquiryService.getUnprocessedCount();
        return ResponseEntity.ok(ApiResponse.success(count, "Unprocessed inquiries count retrieved successfully", request.getRequestURI()));
    }
}


