package com.crm.controller;

import com.crm.dto.ApiResponse;
import com.crm.dto.ContactDTO;
import com.crm.dto.ContactDetailDTO;
import com.crm.dto.LeadSummaryDTO;
import com.crm.service.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/contacts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContactDTO>>> getAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ContactDTO> contacts = contactService.getAllContacts(pageable);
        return ResponseEntity.ok(ApiResponse.success(contacts, "Contacts retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ContactDTO>>> searchContacts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ContactDTO> contacts = contactService.searchContacts(name, email, phone, pageable);
        return ResponseEntity.ok(ApiResponse.success(contacts, "Contacts search completed", request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactDetailDTO>> getContactById(
            @PathVariable Long id,
            HttpServletRequest request) {

        ContactDetailDTO contact = contactService.getContactWithLeads(id);
        return ResponseEntity.ok(ApiResponse.success(contact, "Contact retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/{id}/leads")
    public ResponseEntity<ApiResponse<List<LeadSummaryDTO>>> getContactLeads(
            @PathVariable Long id,
            HttpServletRequest request) {

        List<LeadSummaryDTO> leads = contactService.getContactLeads(id);
        return ResponseEntity.ok(ApiResponse.success(leads, "Contact leads retrieved successfully", request.getRequestURI()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContactDTO>> createContact(
            @Valid @RequestBody ContactDTO contactDTO,
            HttpServletRequest request) {

        // For creating a contact without a lead
        ContactDTO createdContact = contactService.getOrCreateContact(
                contactDTO.getName(),
                contactDTO.getEmail(),
                contactDTO.getPhoneNumber()
        );

        // Update additional fields
        if (contactDTO.getAddress() != null) {
            createdContact = contactService.updateContact(createdContact.getId(), contactDTO);
        }

        return ResponseEntity.ok(ApiResponse.success(createdContact, "Contact created successfully", request.getRequestURI()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactDTO>> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody ContactDTO contactDTO,
            HttpServletRequest request) {

        ContactDTO updatedContact = contactService.updateContact(id, contactDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedContact, "Contact updated successfully", request.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateContact(
            @PathVariable Long id,
            HttpServletRequest request) {

        contactService.deactivateContact(id);
        return ResponseEntity.ok(ApiResponse.success("Contact deactivated successfully", request.getRequestURI()));
    }
}