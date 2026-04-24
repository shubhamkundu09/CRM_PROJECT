package com.crm.service;

import com.crm.dto.PartnerInquiryDTO;
import com.crm.dto.PartnerInquiryResponseDTO;
import com.crm.dto.PartnerInquiryUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PartnerInquiryService {

    PartnerInquiryResponseDTO submitInquiry(PartnerInquiryDTO inquiryDTO);

    Page<PartnerInquiryResponseDTO> getAllInquiries(Pageable pageable);

    Page<PartnerInquiryResponseDTO> searchInquiries(String name, String email, String phone, Boolean isProcessed, Pageable pageable);

    PartnerInquiryResponseDTO getInquiryById(Long id);

    PartnerInquiryResponseDTO updateInquiryStatus(Long id, PartnerInquiryUpdateDTO updateDTO);

    void deleteInquiry(Long id);

    long getUnprocessedCount();
}