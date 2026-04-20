package com.crm.service;

import com.crm.dto.BannerCreateDTO;
import com.crm.dto.BannerDTO;
import com.crm.dto.BannerUpdateDTO;
import com.crm.entity.Banner;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.BannerRepository;
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
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final FileStorageService fileStorageService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public BannerDTO createBanner(BannerCreateDTO createDTO) {
        log.info("Creating new banner with title: {}", createDTO.getTitle());

        String imageUrl = null;
        if (createDTO.getImage() != null && !createDTO.getImage().isEmpty()) {
            try {
                String fileName = fileStorageService.storeBannerImage(createDTO.getImage());
                imageUrl = fileName;
            } catch (IOException e) {
                log.error("Failed to store banner image: {}", e.getMessage());
                throw new RuntimeException("Failed to upload image: " + e.getMessage());
            }
        }

        Banner banner = Banner.builder()
                .title(createDTO.getTitle())
                .imageUrl(imageUrl)
                .isActive(true)
                .displayOrder(0)
                .build();

        Banner savedBanner = bannerRepository.save(banner);
        log.info("Banner created successfully with ID: {}", savedBanner.getId());

        return mapToDTO(savedBanner);
    }

    @Override
    public BannerDTO updateBanner(BannerUpdateDTO updateDTO) {
        log.info("Updating banner with ID: {}", updateDTO.getId());

        Banner banner = bannerRepository.findById(updateDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + updateDTO.getId()));

        // Update title
        if (updateDTO.getTitle() != null) {
            banner.setTitle(updateDTO.getTitle());
        }

        // Update image if new one is provided
        if (updateDTO.getImage() != null && !updateDTO.getImage().isEmpty()) {
            // Delete old image
            if (banner.getImageUrl() != null && !banner.getImageUrl().isEmpty()) {
                try {
                    fileStorageService.deleteFile(banner.getImageUrl());
                } catch (IOException e) {
                    log.warn("Failed to delete old image: {}", e.getMessage());
                }
            }

            // Store new image
            try {
                String fileName = fileStorageService.storeBannerImage(updateDTO.getImage());
                banner.setImageUrl(fileName);
            } catch (IOException e) {
                log.error("Failed to store banner image: {}", e.getMessage());
                throw new RuntimeException("Failed to upload image: " + e.getMessage());
            }
        }

        // Update status
        if (updateDTO.getIsActive() != null) {
            banner.setIsActive(updateDTO.getIsActive());
        }

        // Update display order
        if (updateDTO.getDisplayOrder() != null) {
            banner.setDisplayOrder(updateDTO.getDisplayOrder());
        }

        Banner updatedBanner = bannerRepository.save(banner);
        log.info("Banner updated successfully with ID: {}", updatedBanner.getId());

        return mapToDTO(updatedBanner);
    }

    @Override
    public void deleteBanner(Long id) {
        log.info("Deleting banner with ID: {}", id);

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));

        // Delete image file
        if (banner.getImageUrl() != null && !banner.getImageUrl().isEmpty()) {
            try {
                fileStorageService.deleteFile(banner.getImageUrl());
            } catch (IOException e) {
                log.warn("Failed to delete image file: {}", e.getMessage());
            }
        }

        bannerRepository.delete(banner);
        log.info("Banner deleted successfully with ID: {}", id);
    }

    @Override
    public BannerDTO getBannerById(Long id) {
        log.info("Fetching banner with ID: {}", id);
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));
        return mapToDTO(banner);
    }

    @Override
    public Page<BannerDTO> getAllBanners(Pageable pageable) {
        log.info("Fetching all banners with pagination");
        return bannerRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToDTO);
    }

    @Override
    public List<BannerDTO> getAllBannersList() {
        log.info("Fetching all banners as list");
        return bannerRepository.findAllOrdered()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BannerDTO> getActiveBannersForWebsite() {
        log.info("Fetching active banners for website (latest 4)");
        return bannerRepository.findTop4ByIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BannerDTO toggleBannerStatus(Long id) {
        log.info("Toggling status for banner with ID: {}", id);

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));

        banner.setIsActive(!banner.getIsActive());
        Banner updatedBanner = bannerRepository.save(banner);

        log.info("Banner status toggled to: {} for ID: {}", updatedBanner.getIsActive(), id);
        return mapToDTO(updatedBanner);
    }

    @Override
    public BannerDTO updateBannerOrder(Long id, Integer displayOrder) {
        log.info("Updating display order for banner with ID: {} to {}", id, displayOrder);

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with ID: " + id));

        banner.setDisplayOrder(displayOrder);
        Banner updatedBanner = bannerRepository.save(banner);

        log.info("Display order updated for banner ID: {}", id);
        return mapToDTO(updatedBanner);
    }

    private BannerDTO mapToDTO(Banner banner) {
        return BannerDTO.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .imageUrl(fileStorageService.getFileUrl(banner.getImageUrl()))
                .isActive(banner.getIsActive())
                .displayOrder(banner.getDisplayOrder())
                .createdAt(banner.getCreatedAt() != null ? banner.getCreatedAt().format(DATE_FORMATTER) : null)
                .updatedAt(banner.getUpdatedAt() != null ? banner.getUpdatedAt().format(DATE_FORMATTER) : null)
                .build();
    }
}