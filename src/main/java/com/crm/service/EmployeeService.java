package com.crm.service;

import com.crm.dto.EmployeeDTO;
import com.crm.dto.EmployeeResponseDTO;
import com.crm.dto.PasswordResetDTO;
import com.crm.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeService {
    EmployeeResponseDTO createEmployee(EmployeeDTO employeeDTO);
    EmployeeResponseDTO updateEmployee(Long id, EmployeeDTO employeeDTO);

    // Soft delete - just deactivate
    void deactivateEmployee(Long id);

    // Hard delete - permanently remove from database
    void permanentlyDeleteEmployee(Long id);

    // Reactivate a deactivated employee
    EmployeeResponseDTO reactivateEmployee(Long id);

    // Legacy method - maps to deactivate
    void deleteEmployee(Long id);

    EmployeeResponseDTO getEmployeeById(Long id);
    List<EmployeeResponseDTO> getAllEmployees();
    List<EmployeeResponseDTO> getActiveEmployees();

    // NEW: Get deactivated employees
    List<EmployeeResponseDTO> getDeactivatedEmployees();

    List<EmployeeResponseDTO> getEmployeesByDepartment(String department);
    void resetEmployeePassword(PasswordResetDTO passwordResetDTO);

    // Updated search with isActive filter
    Page<Employee> searchEmployees(String empCode, String name, String department, Boolean isActive, Pageable pageable);
}