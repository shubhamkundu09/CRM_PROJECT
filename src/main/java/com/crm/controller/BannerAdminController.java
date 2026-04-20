package com.crm.controller;

import com.crm.dto.ApiResponse;
import com.crm.dto.BannerCreateDTO;
import com.crm.dto.BannerDTO;
import com.crm.dto.BannerUpdateDTO;
import com.crm.service.BannerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class BannerAdminController {

    private final BannerService bannerService;

    @PostMapping
    public ResponseEntity<ApiResponse<BannerDTO>> createBanner(
            @Valid @ModelAttribute BannerCreateDTO createDTO,
            HttpServletRequest request) {
        log.info("Admin creating new banner with title: {}", createDTO.getTitle());
        BannerDTO createdBanner = bannerService.createBanner(createDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdBanner, "Banner created successfully", request.getRequestURI()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerDTO>> updateBanner(
            @PathVariable Long id,
            @Valid @ModelAttribute BannerUpdateDTO updateDTO,
            HttpServletRequest request) {
        updateDTO.setId(id);
        log.info("Admin updating banner with ID: {}", id);
        BannerDTO updatedBanner = bannerService.updateBanner(updateDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedBanner, "Banner updated successfully", request.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(
            @PathVariable Long id,
            HttpServletRequest request) {
        log.info("Admin deleting banner with ID: {}", id);
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.success("Banner deleted successfully", request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerDTO>> getBannerById(
            @PathVariable Long id,
            HttpServletRequest request) {
        log.info("Admin fetching banner with ID: {}", id);
        BannerDTO banner = bannerService.getBannerById(id);
        return ResponseEntity.ok(ApiResponse.success(banner, "Banner retrieved successfully", request.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BannerDTO>>> getAllBanners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {
        log.info("Admin fetching all banners - page: {}, size: {}", page, size);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BannerDTO> banners = bannerService.getAllBanners(pageable);
        return ResponseEntity.ok(ApiResponse.success(banners, "Banners retrieved successfully", request.getRequestURI()));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<BannerDTO>>> getAllBannersList(HttpServletRequest request) {
        log.info("Admin fetching all banners as list");
        List<BannerDTO> banners = bannerService.getAllBannersList();
        return ResponseEntity.ok(ApiResponse.success(banners, "Banners retrieved successfully", request.getRequestURI()));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<BannerDTO>> toggleBannerStatus(
            @PathVariable Long id,
            HttpServletRequest request) {
        log.info("Admin toggling status for banner with ID: {}", id);
        BannerDTO banner = bannerService.toggleBannerStatus(id);
        return ResponseEntity.ok(ApiResponse.success(banner, "Banner status updated successfully", request.getRequestURI()));
    }

    @PatchMapping("/{id}/order")
    public ResponseEntity<ApiResponse<BannerDTO>> updateBannerOrder(
            @PathVariable Long id,
            @RequestParam Integer displayOrder,
            HttpServletRequest request) {
        log.info("Admin updating display order for banner ID: {} to {}", id, displayOrder);
        BannerDTO banner = bannerService.updateBannerOrder(id, displayOrder);
        return ResponseEntity.ok(ApiResponse.success(banner, "Banner order updated successfully", request.getRequestURI()));
    }
}