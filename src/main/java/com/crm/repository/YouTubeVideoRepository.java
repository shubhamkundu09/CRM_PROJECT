package com.crm.repository;

import com.crm.entity.YouTubeVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface YouTubeVideoRepository extends JpaRepository<YouTubeVideo, Long> {

    List<YouTubeVideo> findByIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc();

    Page<YouTubeVideo> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT v FROM YouTubeVideo v ORDER BY v.displayOrder ASC, v.createdAt DESC")
    List<YouTubeVideo> findAllOrdered();

    List<YouTubeVideo> findTop6ByIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
}