package com.crm.service;

import com.crm.dto.YouTubeVideoCreateDTO;
import com.crm.dto.YouTubeVideoDTO;
import com.crm.dto.YouTubeVideoUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface YouTubeVideoService {

    YouTubeVideoDTO createVideo(YouTubeVideoCreateDTO createDTO);

    YouTubeVideoDTO updateVideo(YouTubeVideoUpdateDTO updateDTO);

    void deleteVideo(Long id);

    YouTubeVideoDTO getVideoById(Long id);

    Page<YouTubeVideoDTO> getAllVideos(Pageable pageable);

    List<YouTubeVideoDTO> getAllVideosList();

    List<YouTubeVideoDTO> getActiveVideosForWebsite();

    YouTubeVideoDTO toggleVideoStatus(Long id);

    YouTubeVideoDTO updateVideoOrder(Long id, Integer displayOrder);
}