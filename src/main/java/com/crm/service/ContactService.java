package com.crm.service;

import com.crm.dto.ContactDTO;
import com.crm.dto.ContactDetailDTO;
import com.crm.dto.LeadSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContactService {

    ContactDTO getOrCreateContact(String name, String email, String phoneNumber);

    ContactDTO getContactById(Long id);

    ContactDetailDTO getContactWithLeads(Long id);

    Page<ContactDTO> getAllContacts(Pageable pageable);

    Page<ContactDTO> searchContacts(String name, String email, String phone, Pageable pageable);

    List<LeadSummaryDTO> getContactLeads(Long contactId);

    ContactDTO updateContact(Long id, ContactDTO contactDTO);

    void deactivateContact(Long id);

    long getTotalContactsCount();
}