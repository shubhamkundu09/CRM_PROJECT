package com.crm.service;

import com.crm.dto.UnifiedLoginRequest;
import com.crm.dto.UnifiedLoginResponse;
import com.crm.entity.Employee;
import com.crm.exception.ResourceNotFoundException;
import com.crm.exception.UnauthorizedException;
import com.crm.repository.EmployeeRepository;
import com.crm.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UnifiedAuthServiceImpl implements UnifiedAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final EmployeeRepository employeeRepository;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public UnifiedLoginResponse authenticate(UnifiedLoginRequest loginRequest) {
        log.info("Unified login attempt for email: {}", loginRequest.getEmail());

        // First check if user exists
        Employee employee = employeeRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + loginRequest.getEmail()));

        // Check if user is active - THIS PREVENTS DEACTIVATED EMPLOYEES FROM LOGGING IN
        if (!employee.getIsActive()) {
            log.warn("Login attempt by deactivated user: {}", loginRequest.getEmail());
            throw new UnauthorizedException("Account is deactivated. Please contact administrator.");
        }

        // Authenticate the user
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Determine role
        String role = determineRole(employee);

        // Generate JWT token
        String jwt = jwtUtils.generateJwtToken(authentication);

        // Check if first login
        boolean isFirstLogin = employee.getIsFirstLogin();

        String message = isFirstLogin ?
                "Login successful. Please change your password." :
                "Login successful";

        log.info("User logged in successfully: {} with role: {}", loginRequest.getEmail(), role);

        return UnifiedLoginResponse.builder()
                .token(jwt)
                .email(loginRequest.getEmail())
                .role(role)
                .message(message)
                .isFirstLogin(isFirstLogin)
                .build();
    }

    private String determineRole(Employee employee) {
        if (adminEmail.equals(employee.getEmail())) {
            return "ADMIN";
        }
        return "EMPLOYEE";
    }
}