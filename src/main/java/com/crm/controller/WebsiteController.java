package com.crm.controller;

import com.crm.dto.*;
import com.crm.service.PartnerBannerService;
import com.crm.service.PartnerInquiryService;
import com.crm.service.WebsiteLeadService;
import com.crm.service.YouTubeVideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/website")
@RequiredArgsConstructor
@Slf4j
public class WebsiteController {

    private final WebsiteLeadService websiteLeadService;
    private final YouTubeVideoService youTubeVideoService;
    private final PartnerInquiryService partnerInquiryService;
    private final PartnerBannerService partnerBannerService;  // Add this

    @PostMapping("/leads")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> submitLead(
            @Valid @RequestBody WebsiteLeadDTO websiteLeadDTO,
            HttpServletRequest request) {
        log.info("New website lead submission from: {}", websiteLeadDTO.getEmail());
        LeadResponseDTO lead = websiteLeadService.submitWebsiteLead(websiteLeadDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(lead,
                        "Lead submitted successfully! Our team will contact you within 24 hours.",
                        request.getRequestURI()));
    }

    // ==================== YOUTUBE VIDEOS FOR WEBSITE ====================

    @GetMapping("/youtube-videos")
    public ResponseEntity<ApiResponse<List<YouTubeVideoDTO>>> getActiveYouTubeVideos(HttpServletRequest request) {
        log.info("Fetching active YouTube videos for website");
        List<YouTubeVideoDTO> videos = youTubeVideoService.getActiveVideosForWebsite();
        return ResponseEntity.ok(ApiResponse.success(videos, "YouTube videos retrieved successfully", request.getRequestURI()));
    }

    // ==================== PARTNER INQUIRY FOR WEBSITE ====================

    @PostMapping("/partner-inquiry")
    public ResponseEntity<ApiResponse<PartnerInquiryResponseDTO>> submitPartnerInquiry(
            @Valid @RequestBody PartnerInquiryDTO inquiryDTO,
            HttpServletRequest request) {
        log.info("New partner inquiry submission from: {}", inquiryDTO.getEmail());
        PartnerInquiryResponseDTO inquiry = partnerInquiryService.submitInquiry(inquiryDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inquiry,
                        "Thank you for your interest in partnering with us! Our team will contact you soon.",
                        request.getRequestURI()));
    }

    // ==================== PARTNER BANNERS FOR WEBSITE ====================

    @GetMapping("/partner-banners")
    public ResponseEntity<ApiResponse<List<PartnerBannerDTO>>> getActivePartnerBanners(HttpServletRequest request) {
        log.info("Fetching active partner banners for website");
        List<PartnerBannerDTO> banners = partnerBannerService.getActivePartnerBannersForWebsite();
        return ResponseEntity.ok(ApiResponse.success(banners, "Partner banners retrieved successfully", request.getRequestURI()));
    }
}