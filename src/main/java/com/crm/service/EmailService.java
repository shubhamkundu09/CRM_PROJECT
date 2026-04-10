package com.crm.service;

import com.crm.dto.WebsiteLeadDTO;
import com.crm.entity.Lead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.admin.notification-email}")
    private String adminEmail;

    @Async
    public void sendWelcomeEmail(String to, String name, String employeeCode, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Welcome to Samriddhi Financial Services - Your Account Details");
            message.setText(String.format("""
                Dear %s,
                
                Welcome to the Samriddhi Financial Services! Your employee account has been created successfully.
                
                Your Account Details:
                ------------------------
                Employee Code: %s
                Email: %s
                Password: %s
                
                Important Notes:
                1. Please change your password after first login
                2. Never share your password with anyone
                3. For security reasons, you will be prompted to change your password on first login
                4. You can login using your email and the password provided above
                
                Login URL: Contact Admin
                
                Best regards,
                Samriddhi Financial Services Admin Team
                """, name, employeeCode, to, password));

            mailSender.send(message);
            log.info("Welcome email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendLeadChangeNotificationToAdmin(Lead oldLead, Lead newLead, String employeeName, String employeeRole, List<String> changes) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setSubject("🔔 LEAD UPDATE NOTIFICATION - " + newLead.getName());

            StringBuilder emailContent = new StringBuilder();
            emailContent.append(String.format("""
                ═══════════════════════════════════════════════════════════
                           LEAD UPDATE NOTIFICATION
                ═══════════════════════════════════════════════════════════
                
                📋 Lead Information:
                ───────────────────
                Lead Name: %s
                Lead ID: %d
                Lead Email: %s
                Lead Phone: %s
                
                👤 Updated By: %s (%s)
                🕐 Update Time: %s
                
                ═══════════════════════════════════════════════════════════
                           CHANGES MADE
                ═══════════════════════════════════════════════════════════
                
                """,
                    newLead.getName(),
                    newLead.getId(),
                    newLead.getEmail(),
                    newLead.getPhoneNumber(),
                    employeeName,
                    employeeRole,
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));

            // List all changes with bullet points
            for (String change : changes) {
                emailContent.append("  • ").append(change).append("\n");
            }

            emailContent.append("\n");
            emailContent.append("╔═══════════════════════════════════════════════════════════╗\n");
            emailContent.append("║                    BEFORE UPDATE (Previous Values)        ║\n");
            emailContent.append("╚═══════════════════════════════════════════════════════════╝\n\n");
            emailContent.append(getLeadDetails(oldLead));

            emailContent.append("\n\n");
            emailContent.append("╔═══════════════════════════════════════════════════════════╗\n");
            emailContent.append("║                    AFTER UPDATE (Current Values)         ║\n");
            emailContent.append("╚═══════════════════════════════════════════════════════════╝\n\n");
            emailContent.append(getLeadDetails(newLead));

            emailContent.append("\n\n");
            emailContent.append("═".repeat(60));
            emailContent.append("\n");
            emailContent.append("📌 Please login to the Samriddhi Financial Services system to view more details.\n");
            emailContent.append("═".repeat(60));
            emailContent.append("\n\nBest regards,\nSamriddhi Financial Services System");

            message.setText(emailContent.toString());
            mailSender.send(message);
            log.info("Lead change notification sent to admin for lead ID: {}", newLead.getId());
        } catch (Exception e) {
            log.error("Failed to send lead change notification to admin: {}", e.getMessage());
        }
    }

    @Async
    public void sendLeadStageChangeNotificationToAdmin(Lead lead, String employeeName, String employeeRole,
                                                       String previousStage, String newStage, String remarks) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setSubject("🔄 LEAD STAGE CHANGE - " + lead.getName());

            String emailContent = String.format("""
                ═══════════════════════════════════════════════════════════
                           LEAD STAGE CHANGE NOTIFICATION
                ═══════════════════════════════════════════════════════════
                
                📋 Lead Information:
                ───────────────────
                Lead Name: %s
                Lead ID: %d
                Lead Email: %s
                Lead Phone: %s
                
                👤 Updated By: %s (%s)
                🕐 Update Time: %s
                
                ═══════════════════════════════════════════════════════════
                           STAGE CHANGE DETAILS
                ═══════════════════════════════════════════════════════════
                
                Previous Stage: %s
                New Stage: %s
                
                📝 Remarks: %s
                
                ═══════════════════════════════════════════════════════════
                           CURRENT LEAD DETAILS
                ═══════════════════════════════════════════════════════════
                
                Name: %s
                Email: %s
                Phone: %s
                Lead Type: %s
                Current Stage: %s
                Next Follow-up: %s
                Next Follow-up Description: %s
                Assigned To: %s
                
                """,
                    lead.getName(),
                    lead.getId(),
                    lead.getEmail(),
                    lead.getPhoneNumber(),
                    employeeName,
                    employeeRole,
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    previousStage,
                    newStage,
                    remarks != null ? remarks : "No remarks provided",
                    lead.getName(),
                    lead.getEmail(),
                    lead.getPhoneNumber(),
                    lead.getLeadType() != null ? lead.getLeadType().getDescription() : "Not set",
                    lead.getLeadStage() != null ? lead.getLeadStage().getDisplayName() : "Not set",
                    lead.getNextFollowUpDate() != null ? lead.getNextFollowUpDate().toString() : "Not set",
                    lead.getNextFollowUp() != null ? lead.getNextFollowUp() : "Not set",
                    lead.getAssignedEmployee() != null ? lead.getAssignedEmployee().getFirstName() + " " + lead.getAssignedEmployee().getLastName() : "Not assigned"
            );

            message.setText(emailContent);
            mailSender.send(message);
            log.info("Lead stage change notification sent to admin for lead ID: {}", lead.getId());
        } catch (Exception e) {
            log.error("Failed to send lead stage change notification to admin: {}", e.getMessage());
        }
    }

    private String getLeadDetails(Lead lead) {
        if (lead == null) return "Lead not found";

        StringBuilder details = new StringBuilder();
        details.append(String.format("""
            ┌─────────────────────────────────────────────────────────────┐
            │ Basic Information                                           │
            ├─────────────────────────────────────────────────────────────┤
            │ Name: %-45s │
            │ Email: %-45s │
            │ Phone: %-45s │
            │ Lead Type: %-42s │
            │ Lead Stage: %-41s │
            │ Source: %-45s │
            └─────────────────────────────────────────────────────────────┘
            
            ┌─────────────────────────────────────────────────────────────┐
            │ Assignment Details                                          │
            ├─────────────────────────────────────────────────────────────┤
            │ Assigned To: %-41s │
            └─────────────────────────────────────────────────────────────┘
            
            ┌─────────────────────────────────────────────────────────────┐
            │ Follow-up Information                                       │
            ├─────────────────────────────────────────────────────────────┤
            │ Next Follow-up Date: %-35s │
            │ Next Follow-up Desc: %-35s │
            │ Remarks: %-46s │
            └─────────────────────────────────────────────────────────────┘
            """,
                truncate(lead.getName(), 45),
                truncate(lead.getEmail(), 45),
                truncate(lead.getPhoneNumber(), 45),
                truncate(lead.getLeadType() != null ? lead.getLeadType().getDescription() : "Not set", 42),
                truncate(lead.getLeadStage() != null ? lead.getLeadStage().getDisplayName() : "Not set", 41),
                truncate(lead.getSource() != null ? lead.getSource() : "Not set", 45),
                truncate(lead.getAssignedEmployee() != null ? lead.getAssignedEmployee().getFirstName() + " " + lead.getAssignedEmployee().getLastName() : "Not assigned", 41),
                lead.getNextFollowUpDate() != null ? lead.getNextFollowUpDate().toString() : "Not set",
                truncate(lead.getNextFollowUp() != null ? lead.getNextFollowUp() : "Not set", 35),
                truncate(lead.getRemarks() != null ? lead.getRemarks() : "Not set", 46)
        ));

        // Add service details if present
        if (lead.getInterestedService() != null || lead.getServiceSubcategory() != null) {
            details.append("\n┌─────────────────────────────────────────────────────────────┐\n");
            details.append("│ Service Details                                           │\n");
            details.append("├─────────────────────────────────────────────────────────────┤\n");
            if (lead.getInterestedService() != null) {
                details.append(String.format("│ Interested Service: %-36s │\n", truncate(lead.getInterestedService().getDisplayName(), 36)));
            }
            if (lead.getServiceSubcategory() != null) {
                details.append(String.format("│ Service Subcategory: %-34s │\n", truncate(lead.getServiceSubcategory().getDisplayName(), 34)));
            }
            if (lead.getServiceSubSubcategory() != null) {
                details.append(String.format("│ Service Sub-subcategory: %-31s │\n", truncate(lead.getServiceSubSubcategory().getDisplayName(), 31)));
            }
            if (lead.getServiceDescription() != null) {
                details.append(String.format("│ Service Description: %-34s │\n", truncate(lead.getServiceDescription(), 34)));
            }
            details.append("└─────────────────────────────────────────────────────────────┘\n");
        }

        // Add statistics
        details.append(String.format("""
            
            ┌─────────────────────────────────────────────────────────────┐
            │ Statistics                                                  │
            ├─────────────────────────────────────────────────────────────┤
            │ Calls Made: %-40d │
            │ Meetings Booked: %-37d │
            │ Meetings Completed: %-35d │
            │ Total Updates: %-39d │
            │ Last Updated By: %-37s │
            └─────────────────────────────────────────────────────────────┘
            """,
                lead.getCallsMadeCount() != null ? lead.getCallsMadeCount() : 0,
                lead.getMeetingsBookedCount() != null ? lead.getMeetingsBookedCount() : 0,
                lead.getMeetingsDoneCount() != null ? lead.getMeetingsDoneCount() : 0,
                lead.getUpdateCount() != null ? lead.getUpdateCount() : 0,
                truncate(lead.getLastUpdatedBy() != null ? lead.getLastUpdatedBy() : "System", 37)
        ));

        return details.toString();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "Not set";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    @Async
    public void sendWebsiteLeadNotification(WebsiteLeadDTO leadDTO) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setSubject("🌐 NEW WEBSITE LEAD - " + leadDTO.getName());
            message.setText(String.format("""
                ═══════════════════════════════════════════════════════════
                           NEW WEBSITE LEAD SUBMISSION
                ═══════════════════════════════════════════════════════════
                
                Customer Details:
                ─────────────────
                Name: %s
                Email: %s
                Phone: %s
                Service: %s
                
                Message:
                ─────────────────
                %s
                
                ═══════════════════════════════════════════════════════════
                📌 Please login to the system to assign and follow up with this lead.
                ═══════════════════════════════════════════════════════════
                """,
                    leadDTO.getName(),
                    leadDTO.getEmail(),
                    leadDTO.getPhoneNumber(),
                    leadDTO.getInterestedService() != null ? leadDTO.getInterestedService().getDisplayName() : "Not specified",
                    leadDTO.getRemarks() != null ? leadDTO.getRemarks() : "No message provided"
            ));
            mailSender.send(message);
            log.info("Website lead notification sent to admin: {}", adminEmail);
        } catch (Exception e) {
            log.error("Failed to send website lead notification: {}", e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String newPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("🔐 Password Reset - Samriddhi Financial Services");
            message.setText(String.format("""
                Dear %s,
                
                Your password has been reset by the administrator.
                
                New Login Credentials:
                ─────────────────────
                Email: %s
                New Password: %s
                
                Security Instructions:
                ─────────────────────
                1. Please change this password after logging in
                2. Do not share this password with anyone
                3. If you didn't request this reset, please contact the administrator immediately
                
                Best regards,
                Samriddhi Financial Services Admin Team
                """, name, to, newPassword));
            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendLeadAssignmentEmail(String to, String employeeName, String leadName, String leadType) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("📋 New Lead Assigned - Samriddhi Financial Services");
            message.setText(String.format("""
                Dear %s,
                
                A new lead has been assigned to you.
                
                Lead Details:
                ─────────────
                Name: %s
                Type: %s
                
                Please log in to the Samriddhi Financial Services system to view more details and take necessary action.
                
                Best regards,
                Samriddhi Financial Services Admin Team
                """, employeeName, leadName, leadType));
            mailSender.send(message);
            log.info("Lead assignment email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send lead assignment email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendPasswordChangeConfirmation(String to, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("✅ Password Changed Successfully - Samriddhi Financial Services");
            message.setText(String.format("""
                Dear %s,
                
                This is to confirm that your password has been successfully changed.
                
                If you did not make this change, please contact the system administrator immediately.
                
                Best regards,
                Samriddhi Financial Services Admin Team
                """, name));
            mailSender.send(message);
            log.info("Password change confirmation sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password change confirmation to {}: {}", to, e.getMessage());
        }
    }
}