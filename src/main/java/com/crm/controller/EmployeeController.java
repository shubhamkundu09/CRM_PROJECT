package com.crm.controller;

import com.crm.dto.ApiResponse;
import com.crm.dto.EmployeeDTO;
import com.crm.dto.EmployeeResponseDTO;
import com.crm.dto.PasswordResetDTO;
import com.crm.entity.Employee;
import com.crm.service.EmployeeService;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> createEmployee(
            @Valid @RequestBody EmployeeDTO employeeDTO,
            HttpServletRequest request) {
        log.info("Creating new employee with email: {}", employeeDTO.getEmail());
        EmployeeResponseDTO createdEmployee = employeeService.createEmployee(employeeDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdEmployee, "Employee created successfully", request.getRequestURI()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> updateEmployee(
            @PathVariable String id,
            @Valid @RequestBody EmployeeDTO employeeDTO,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Updating employee with ID: {}", decryptedId);
        EmployeeResponseDTO updatedEmployee = employeeService.updateEmployee(decryptedId, employeeDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedEmployee, "Employee updated successfully", request.getRequestURI()));
    }

    // NEW: Soft delete (deactivate) - Employee cannot login but data remains
    @DeleteMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateEmployee(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Deactivating employee with ID: {}", decryptedId);
        employeeService.deactivateEmployee(decryptedId);
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated successfully. Employee can no longer login.", request.getRequestURI()));
    }

    // NEW: Permanent delete (hard delete) - Completely remove from database
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteEmployee(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Permanently deleting employee with ID: {}", decryptedId);
        employeeService.permanentlyDeleteEmployee(decryptedId);
        return ResponseEntity.ok(ApiResponse.success("Employee permanently deleted from system.", request.getRequestURI()));
    }

    // NEW: Reactivate a deactivated employee
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> reactivateEmployee(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Reactivating employee with ID: {}", decryptedId);
        EmployeeResponseDTO reactivatedEmployee = employeeService.reactivateEmployee(decryptedId);
        return ResponseEntity.ok(ApiResponse.success(reactivatedEmployee, "Employee reactivated successfully", request.getRequestURI()));
    }

    // Keep original delete method for backward compatibility (maps to deactivate)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Soft deleting employee with ID: {}", decryptedId);
        employeeService.deactivateEmployee(decryptedId);
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated successfully", request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getEmployeeById(
            @PathVariable String id,
            HttpServletRequest request) {
        Long decryptedId = CryptoUtil.decryptToLong(id);
        log.info("Fetching employee with ID: {}", decryptedId);
        EmployeeResponseDTO employee = employeeService.getEmployeeById(decryptedId);
        return ResponseEntity.ok(ApiResponse.success(employee, "Employee retrieved successfully", request.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getAllEmployees(HttpServletRequest request) {
        log.info("Fetching all employees");
        List<EmployeeResponseDTO> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(ApiResponse.success(employees, "Employees retrieved successfully", request.getRequestURI()));
    }

    // NEW: Get only active employees
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getActiveEmployees(HttpServletRequest request) {
        log.info("Fetching active employees");
        List<EmployeeResponseDTO> employees = employeeService.getActiveEmployees();
        return ResponseEntity.ok(ApiResponse.success(employees, "Active employees retrieved successfully", request.getRequestURI()));
    }

    // NEW: Get only deactivated employees
    @GetMapping("/deactivated")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getDeactivatedEmployees(HttpServletRequest request) {
        log.info("Fetching deactivated employees");
        List<EmployeeResponseDTO> employees = employeeService.getDeactivatedEmployees();
        return ResponseEntity.ok(ApiResponse.success(employees, "Deactivated employees retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getEmployeesByDepartment(
            @PathVariable String department,
            HttpServletRequest request) {
        log.info("Fetching employees from department: {}", department);
        List<EmployeeResponseDTO> employees = employeeService.getEmployeesByDepartment(department);
        return ResponseEntity.ok(ApiResponse.success(employees, "Employees retrieved successfully for department: " + department, request.getRequestURI()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetEmployeePassword(
            @Valid @RequestBody PasswordResetDTO passwordResetDTO,
            HttpServletRequest request) {
        log.info("Admin resetting password for employee: {}", passwordResetDTO.getEmail());
        employeeService.resetEmployeePassword(passwordResetDTO);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. New password has been sent to employee's email.", request.getRequestURI()));
    }

    @GetMapping("/search")
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
}