package com.crm.repository;

import com.crm.entity.PartnerInquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PartnerInquiryRepository extends JpaRepository<PartnerInquiry, Long> {

    Page<PartnerInquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<PartnerInquiry> findByIsProcessedOrderByCreatedAtDesc(Boolean isProcessed, Pageable pageable);

    @Query("SELECT p FROM PartnerInquiry p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:email IS NULL OR LOWER(p.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:phone IS NULL OR p.phoneNumber LIKE CONCAT('%', :phone, '%')) AND " +
            "(:isProcessed IS NULL OR p.isProcessed = :isProcessed)")
    Page<PartnerInquiry> searchPartnerInquiries(
            @Param("name") String name,
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("isProcessed") Boolean isProcessed,
            Pageable pageable
    );

    long countByIsProcessedFalse();
}