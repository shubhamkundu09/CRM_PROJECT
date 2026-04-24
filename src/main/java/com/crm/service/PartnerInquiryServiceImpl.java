package com.crm.service;

import com.crm.dto.PartnerInquiryDTO;
import com.crm.dto.PartnerInquiryResponseDTO;
import com.crm.dto.PartnerInquiryUpdateDTO;
import com.crm.entity.PartnerInquiry;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.PartnerInquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PartnerInquiryServiceImpl implements PartnerInquiryService {

    private final PartnerInquiryRepository partnerInquiryRepository;
    private final EmailService emailService;

    @Override
    public PartnerInquiryResponseDTO submitInquiry(PartnerInquiryDTO inquiryDTO) {
        log.info("Submitting partner inquiry from: {}", inquiryDTO.getEmail());

        PartnerInquiry inquiry = PartnerInquiry.builder()
                .name(inquiryDTO.getName())
                .email(inquiryDTO.getEmail().toLowerCase())
                .phoneNumber(inquiryDTO.getPhoneNumber())
                .isProcessed(false)
                .build();

        PartnerInquiry savedInquiry = partnerInquiryRepository.save(inquiry);

        // Send notification email to admin
        sendAdminNotification(savedInquiry);

        log.info("Partner inquiry submitted successfully with ID: {}", savedInquiry.getId());
        return mapToResponseDTO(savedInquiry);
    }

    @Override
    public Page<PartnerInquiryResponseDTO> getAllInquiries(Pageable pageable) {
        log.info("Fetching all partner inquiries with pagination");
        return partnerInquiryRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    public Page<PartnerInquiryResponseDTO> searchInquiries(String name, String email, String phone,
                                                           Boolean isProcessed, Pageable pageable) {
        log.info("Searching partner inquiries - name: {}, email: {}, phone: {}, processed: {}",
                name, email, phone, isProcessed);

        // Normalize string parameters
        name = normalize(name);
        email = normalize(email);
        phone = normalize(phone);

        return partnerInquiryRepository.searchPartnerInquiries(name, email, phone, isProcessed, pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    public PartnerInquiryResponseDTO getInquiryById(Long id) {
        log.info("Fetching partner inquiry with ID: {}", id);
        PartnerInquiry inquiry = partnerInquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner inquiry not found with ID: " + id));
        return mapToResponseDTO(inquiry);
    }

    @Override
    public PartnerInquiryResponseDTO updateInquiryStatus(Long id, PartnerInquiryUpdateDTO updateDTO) {
        log.info("Updating partner inquiry status for ID: {} to processed: {}", id, updateDTO.getIsProcessed());

        PartnerInquiry inquiry = partnerInquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner inquiry not found with ID: " + id));

        if (updateDTO.getIsProcessed() != null) {
            inquiry.setIsProcessed(updateDTO.getIsProcessed());
        }

        PartnerInquiry updatedInquiry = partnerInquiryRepository.save(inquiry);
        log.info("Partner inquiry status updated successfully for ID: {}", id);

        return mapToResponseDTO(updatedInquiry);
    }

    @Override
    public void deleteInquiry(Long id) {
        log.info("Deleting partner inquiry with ID: {}", id);

        PartnerInquiry inquiry = partnerInquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner inquiry not found with ID: " + id));

        partnerInquiryRepository.delete(inquiry);
        log.info("Partner inquiry deleted successfully with ID: {}", id);
    }

    @Override
    public long getUnprocessedCount() {
        return partnerInquiryRepository.countByIsProcessedFalse();
    }

    private void sendAdminNotification(PartnerInquiry inquiry) {
        try {
            String adminEmail = "redcircle0908@gmail.com"; // Your admin email
            String subject = "🤝 New Partner Inquiry - " + inquiry.getName();
            String message = String.format("""
                ═══════════════════════════════════════════════════════════
                           NEW PARTNER INQUIRY
                ═══════════════════════════════════════════════════════════
                
                Inquiry Details:
                ─────────────────
                Name: %s
                Email: %s
                Phone: %s
                Submitted At: %s
                
                ═══════════════════════════════════════════════════════════
                📌 Please login to the admin dashboard to view and process this inquiry.
                ═══════════════════════════════════════════════════════════
                """,
                    inquiry.getName(),
                    inquiry.getEmail(),
                    inquiry.getPhoneNumber(),
                    inquiry.getCreatedAt()
            );

            // Simple email sending without using JavaMailSender directly
            // You can use your existing emailService if it has a generic send method
            log.info("Notification would be sent to admin for partner inquiry from: {}", inquiry.getEmail());
            log.info("Email content: \n{}", message);

            // If your EmailService has a generic method, uncomment:
            // emailService.sendSimpleEmail(adminEmail, subject, message);

        } catch (Exception e) {
            log.error("Failed to send admin notification for partner inquiry: {}", e.getMessage());
        }
    }

    private String normalize(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    private PartnerInquiryResponseDTO mapToResponseDTO(PartnerInquiry inquiry) {
        return PartnerInquiryResponseDTO.builder()
                .id(inquiry.getId())
                .name(inquiry.getName())
                .email(inquiry.getEmail())
                .phoneNumber(inquiry.getPhoneNumber())
                .isProcessed(inquiry.getIsProcessed())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}