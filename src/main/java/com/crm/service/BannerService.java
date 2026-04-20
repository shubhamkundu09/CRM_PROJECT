package com.crm.service;

import com.crm.dto.BannerCreateDTO;
import com.crm.dto.BannerDTO;
import com.crm.dto.BannerUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BannerService {

    BannerDTO createBanner(BannerCreateDTO createDTO);

    BannerDTO updateBanner(BannerUpdateDTO updateDTO);

    void deleteBanner(Long id);

    BannerDTO getBannerById(Long id);

    Page<BannerDTO> getAllBanners(Pageable pageable);

    List<BannerDTO> getAllBannersList();

    List<BannerDTO> getActiveBannersForWebsite();

    BannerDTO toggleBannerStatus(Long id);

    BannerDTO updateBannerOrder(Long id, Integer displayOrder);
}