package com.crm.controller;

import com.crm.dto.ApiResponse;
import com.crm.dto.BannerDTO;
import com.crm.service.BannerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/website/banners")
@RequiredArgsConstructor
@Slf4j
public class BannerPublicController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerDTO>>> getActiveBanners(HttpServletRequest request) {
        log.info("Fetching active banners for website");
        List<BannerDTO> banners = bannerService.getActiveBannersForWebsite();
        return ResponseEntity.ok(ApiResponse.success(banners, "Banners retrieved successfully", request.getRequestURI()));
    }
}