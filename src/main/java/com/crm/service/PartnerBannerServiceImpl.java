package com.crm.service;

import com.crm.dto.PartnerBannerCreateDTO;
import com.crm.dto.PartnerBannerDTO;
import com.crm.dto.PartnerBannerUpdateDTO;
import com.crm.entity.PartnerBanner;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.PartnerBannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PartnerBannerServiceImpl implements PartnerBannerService {

    private final PartnerBannerRepository partnerBannerRepository;
    private final FileStorageService fileStorageService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PartnerBannerDTO createPartnerBanner(PartnerBannerCreateDTO createDTO) {
        log.info("Creating new partner banner with title: {}", createDTO.getTitle());

        String imageUrl = null;
        if (createDTO.getImage() != null && !createDTO.getImage().isEmpty()) {
            try {
                String fileName = fileStorageService.storePartnerBannerImage(createDTO.getImage());
                imageUrl = fileName;
            } catch (IOException e) {
                log.error("Failed to store partner banner image: {}", e.getMessage());
                throw new RuntimeException("Failed to upload image: " + e.getMessage());
            }
        }

        PartnerBanner partnerBanner = PartnerBanner.builder()
                .title(createDTO.getTitle())
                .imageUrl(imageUrl)
                .isActive(true)
                .build();

        PartnerBanner savedPartnerBanner = partnerBannerRepository.save(partnerBanner);
        log.info("Partner banner created successfully with ID: {}", savedPartnerBanner.getId());

        return mapToDTO(savedPartnerBanner);
    }

    @Override
    public PartnerBannerDTO updatePartnerBanner(PartnerBannerUpdateDTO updateDTO) {
        log.info("Updating partner banner with ID: {}", updateDTO.getId());

        PartnerBanner partnerBanner = partnerBannerRepository.findById(updateDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner banner not found with ID: " + updateDTO.getId()));

        // Update title
        if (updateDTO.getTitle() != null) {
            partnerBanner.setTitle(updateDTO.getTitle());
        }

        // Update image if new one is provided
        if (updateDTO.getImage() != null && !updateDTO.getImage().isEmpty()) {
            // Delete old image
            if (partnerBanner.getImageUrl() != null && !partnerBanner.getImageUrl().isEmpty()) {
                try {
                    fileStorageService.deleteFile(partnerBanner.getImageUrl());
                } catch (IOException e) {
                    log.warn("Failed to delete old image: {}", e.getMessage());
                }
            }

            // Store new image
            try {
                String fileName = fileStorageService.storePartnerBannerImage(updateDTO.getImage());
                partnerBanner.setImageUrl(fileName);
            } catch (IOException e) {
                log.error("Failed to store partner banner image: {}", e.getMessage());
                throw new RuntimeException("Failed to upload image: " + e.getMessage());
            }
        }

        // Update status
        if (updateDTO.getIsActive() != null) {
            partnerBanner.setIsActive(updateDTO.getIsActive());
        }

        PartnerBanner updatedPartnerBanner = partnerBannerRepository.save(partnerBanner);
        log.info("Partner banner updated successfully with ID: {}", updatedPartnerBanner.getId());

        return mapToDTO(updatedPartnerBanner);
    }

    @Override
    public void deletePartnerBanner(Long id) {
        log.info("Deleting partner banner with ID: {}", id);

        PartnerBanner partnerBanner = partnerBannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner banner not found with ID: " + id));

        // Delete image file
        if (partnerBanner.getImageUrl() != null && !partnerBanner.getImageUrl().isEmpty()) {
            try {
                fileStorageService.deleteFile(partnerBanner.getImageUrl());
            } catch (IOException e) {
                log.warn("Failed to delete image file: {}", e.getMessage());
            }
        }

        partnerBannerRepository.delete(partnerBanner);
        log.info("Partner banner deleted successfully with ID: {}", id);
    }

    @Override
    public PartnerBannerDTO getPartnerBannerById(Long id) {
        log.info("Fetching partner banner with ID: {}", id);
        PartnerBanner partnerBanner = partnerBannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner banner not found with ID: " + id));
        return mapToDTO(partnerBanner);
    }

    @Override
    public Page<PartnerBannerDTO> getAllPartnerBanners(Pageable pageable) {
        log.info("Fetching all partner banners with pagination");
        return partnerBannerRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToDTO);
    }

    @Override
    public List<PartnerBannerDTO> getAllPartnerBannersList() {
        log.info("Fetching all partner banners as list");
        return partnerBannerRepository.findAllOrdered()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PartnerBannerDTO> getActivePartnerBannersForWebsite() {
        log.info("Fetching active partner banners for website");
        return partnerBannerRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PartnerBannerDTO togglePartnerBannerStatus(Long id) {
        log.info("Toggling status for partner banner with ID: {}", id);

        PartnerBanner partnerBanner = partnerBannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner banner not found with ID: " + id));

        partnerBanner.setIsActive(!partnerBanner.getIsActive());
        PartnerBanner updatedPartnerBanner = partnerBannerRepository.save(partnerBanner);

        log.info("Partner banner status toggled to: {} for ID: {}", updatedPartnerBanner.getIsActive(), id);
        return mapToDTO(updatedPartnerBanner);
    }

    private PartnerBannerDTO mapToDTO(PartnerBanner partnerBanner) {
        return PartnerBannerDTO.builder()
                .id(partnerBanner.getId())
                .title(partnerBanner.getTitle())
                .imageUrl(fileStorageService.getFileUrl(partnerBanner.getImageUrl()))
                .isActive(partnerBanner.getIsActive())
                .createdAt(partnerBanner.getCreatedAt() != null ? partnerBanner.getCreatedAt().format(DATE_FORMATTER) : null)
                .updatedAt(partnerBanner.getUpdatedAt() != null ? partnerBanner.getUpdatedAt().format(DATE_FORMATTER) : null)
                .build();
    }
}