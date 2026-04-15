package com.crm.service;

import com.crm.dto.ContactDTO;
import com.crm.dto.ContactDetailDTO;
import com.crm.dto.LeadSummaryDTO;
import com.crm.entity.Contact;
import com.crm.entity.Lead;
import com.crm.exception.DuplicateResourceException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;

    @Override
    public ContactDTO getOrCreateContact(String name, String email, String phoneNumber) {
        log.info("Getting or creating contact with email: {}, phone: {}", email, phoneNumber);

        Contact existingContact = contactRepository.findByEmailIgnoreCaseOrPhoneNumber(email, phoneNumber)
                .orElse(null);

        if (existingContact != null) {
            log.info("Found existing contact with ID: {}", existingContact.getId());
            return mapToDTO(existingContact);
        }

        Contact newContact = Contact.builder()
                .name(name)
                .email(email.toLowerCase())
                .phoneNumber(phoneNumber)
                .isActive(true)
                .build();

        Contact savedContact = contactRepository.save(newContact);
        log.info("Created new contact with ID: {}", savedContact.getId());

        return mapToDTO(savedContact);
    }

    @Override
    public ContactDTO getContactById(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with ID: " + id));
        return mapToDTO(contact);
    }

    @Override
    public ContactDetailDTO getContactWithLeads(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with ID: " + id));

        ContactDetailDTO dto = mapToDetailDTO(contact);

        List<LeadSummaryDTO> leadSummaries = contact.getLeads().stream()
                .filter(Lead::getIsActive)
                .map(this::mapToLeadSummaryDTO)
                .collect(Collectors.toList());

        dto.setLeads(leadSummaries);
        return dto;
    }

    @Override
    public Page<ContactDTO> getAllContacts(Pageable pageable) {
        return contactRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    @Override
    public Page<ContactDTO> searchContacts(String name, String email, String phone, Pageable pageable) {
        return contactRepository.searchContacts(name, email, phone, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public List<LeadSummaryDTO> getContactLeads(Long contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with ID: " + contactId));

        return contact.getLeads().stream()
                .filter(Lead::getIsActive)
                .map(this::mapToLeadSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ContactDTO updateContact(Long id, ContactDTO contactDTO) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with ID: " + id));

        if (!contact.getEmail().equalsIgnoreCase(contactDTO.getEmail()) &&
                contactRepository.existsByEmail(contactDTO.getEmail().toLowerCase())) {
            throw new DuplicateResourceException("Email already exists: " + contactDTO.getEmail());
        }

        if (!contact.getPhoneNumber().equals(contactDTO.getPhoneNumber()) &&
                contactRepository.existsByPhoneNumber(contactDTO.getPhoneNumber())) {
            throw new DuplicateResourceException("Phone number already exists: " + contactDTO.getPhoneNumber());
        }

        contact.setName(contactDTO.getName());
        contact.setEmail(contactDTO.getEmail().toLowerCase());
        contact.setPhoneNumber(contactDTO.getPhoneNumber());
        contact.setAddress(contactDTO.getAddress());

        Contact updatedContact = contactRepository.save(contact);
        log.info("Updated contact with ID: {}", id);

        return mapToDTO(updatedContact);
    }

    @Override
    public void deactivateContact(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with ID: " + id));

        contact.setIsActive(false);
        contactRepository.save(contact);
        log.info("Deactivated contact with ID: {}", id);
    }

    @Override
    public long getTotalContactsCount() {
        return contactRepository.count();
    }

    private ContactDTO mapToDTO(Contact contact) {
        return ContactDTO.builder()
                .id(contact.getId())
                .name(contact.getName())
                .email(contact.getEmail())
                .phoneNumber(contact.getPhoneNumber())
                .address(contact.getAddress())
                .isActive(contact.getIsActive())
                .totalLeads(contact.getLeads() != null ? contact.getLeads().size() : 0)
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }

    private ContactDetailDTO mapToDetailDTO(Contact contact) {
        return ContactDetailDTO.builder()
                .id(contact.getId())
                .name(contact.getName())
                .email(contact.getEmail())
                .phoneNumber(contact.getPhoneNumber())
                .address(contact.getAddress())
                .isActive(contact.getIsActive())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }

    private LeadSummaryDTO mapToLeadSummaryDTO(Lead lead) {
        return LeadSummaryDTO.builder()
                .id(lead.getId())
                .name(lead.getName())
                .email(lead.getEmail())
                .phoneNumber(lead.getPhoneNumber())
                .leadType(lead.getLeadType() != null ? lead.getLeadType().toString() : "N/A")
                .leadStage(lead.getLeadStage() != null ? lead.getLeadStage().toString() : "N/A")
                .nextFollowUpDate(lead.getNextFollowUpDate())
                .source(lead.getSource())
                .createdAt(lead.getCreatedAt())
                .assignedEmployeeName(lead.getAssignedEmployee() != null ?
                        lead.getAssignedEmployee().getFirstName() + " " + lead.getAssignedEmployee().getLastName() : "Unassigned")
                .build();
    }
}