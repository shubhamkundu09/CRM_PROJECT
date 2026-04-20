package com.crm.repository;

import com.crm.entity.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc();

    Page<Banner> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT b FROM Banner b ORDER BY b.displayOrder ASC, b.createdAt DESC")
    List<Banner> findAllOrdered();

    List<Banner> findTop4ByIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
}