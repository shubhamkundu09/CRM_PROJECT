package com.crm.repository;

import com.crm.entity.PartnerBanner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerBannerRepository extends JpaRepository<PartnerBanner, Long> {

    List<PartnerBanner> findByIsActiveTrueOrderByCreatedAtDesc();

    Page<PartnerBanner> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM PartnerBanner p ORDER BY p.createdAt DESC")
    List<PartnerBanner> findAllOrdered();

    List<PartnerBanner> findTop6ByIsActiveTrueOrderByCreatedAtDesc();
}