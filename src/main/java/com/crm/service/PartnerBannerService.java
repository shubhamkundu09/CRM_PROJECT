package com.crm.service;

import com.crm.dto.PartnerBannerCreateDTO;
import com.crm.dto.PartnerBannerDTO;
import com.crm.dto.PartnerBannerUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PartnerBannerService {

    PartnerBannerDTO createPartnerBanner(PartnerBannerCreateDTO createDTO);

    PartnerBannerDTO updatePartnerBanner(PartnerBannerUpdateDTO updateDTO);

    void deletePartnerBanner(Long id);

    PartnerBannerDTO getPartnerBannerById(Long id);

    Page<PartnerBannerDTO> getAllPartnerBanners(Pageable pageable);

    List<PartnerBannerDTO> getAllPartnerBannersList();

    List<PartnerBannerDTO> getActivePartnerBannersForWebsite();

    PartnerBannerDTO togglePartnerBannerStatus(Long id);
}