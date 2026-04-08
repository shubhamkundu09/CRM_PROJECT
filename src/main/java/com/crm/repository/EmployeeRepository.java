package com.crm.repository;

import com.crm.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmployeeCode(String employeeCode);

    List<Employee> findByDepartment(String department);

    List<Employee> findByIsActiveTrue();

    List<Employee> findByIsActiveFalse();

    List<Employee> findByFirstNameContainingOrLastNameContaining(String firstName, String lastName);

    @Query("SELECT e FROM Employee e WHERE e.department = :dept AND e.isActive = true")
    List<Employee> findActiveEmployeesByDepartment(@Param("dept") String department);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    @Query("""
        SELECT e FROM Employee e
        WHERE 
            (:empCode IS NULL OR TRIM(:empCode) = '' OR e.employeeCode = :empCode)
        AND (:name IS NULL OR TRIM(:name) = '' OR 
                LOWER(e.firstName) LIKE LOWER(CONCAT('%', TRIM(:name), '%')) OR
                LOWER(e.lastName) LIKE LOWER(CONCAT('%', TRIM(:name), '%')))
        AND (:department IS NULL OR TRIM(:department) = '' OR 
                LOWER(e.department) = LOWER(TRIM(:department)))
        AND (:isActive IS NULL OR e.isActive = :isActive)
        """)
    Page<Employee> searchEmployees(
            @Param("empCode") String empCode,
            @Param("name") String name,
            @Param("department") String department,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}