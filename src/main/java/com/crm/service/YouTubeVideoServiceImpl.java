package com.crm.service;

import com.crm.dto.YouTubeVideoCreateDTO;
import com.crm.dto.YouTubeVideoDTO;
import com.crm.dto.YouTubeVideoUpdateDTO;
import com.crm.entity.YouTubeVideo;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.YouTubeVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class YouTubeVideoServiceImpl implements YouTubeVideoService {

    private final YouTubeVideoRepository youTubeVideoRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public YouTubeVideoDTO createVideo(YouTubeVideoCreateDTO createDTO) {
        log.info("Creating new YouTube video with title: {}", createDTO.getTitle());

        // Validate and clean YouTube iframe URL
        String cleanedIframeUrl = cleanYouTubeUrl(createDTO.getIframeUrl());

        YouTubeVideo video = YouTubeVideo.builder()
                .title(createDTO.getTitle())
                .iframeUrl(cleanedIframeUrl)
                .description(createDTO.getDescription())
                .duration(createDTO.getDuration())
                .isActive(true)
                .displayOrder(createDTO.getDisplayOrder() != null ? createDTO.getDisplayOrder() : 0)
                .build();

        YouTubeVideo savedVideo = youTubeVideoRepository.save(video);
        log.info("YouTube video created successfully with ID: {}", savedVideo.getId());

        return mapToDTO(savedVideo);
    }

    @Override
    public YouTubeVideoDTO updateVideo(YouTubeVideoUpdateDTO updateDTO) {
        log.info("Updating YouTube video with ID: {}", updateDTO.getId());

        YouTubeVideo video = youTubeVideoRepository.findById(updateDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("YouTube video not found with ID: " + updateDTO.getId()));

        if (updateDTO.getTitle() != null) {
            video.setTitle(updateDTO.getTitle());
        }

        if (updateDTO.getIframeUrl() != null) {
            String cleanedIframeUrl = cleanYouTubeUrl(updateDTO.getIframeUrl());
            video.setIframeUrl(cleanedIframeUrl);
        }

        if (updateDTO.getDescription() != null) {
            video.setDescription(updateDTO.getDescription());
        }

        if (updateDTO.getDuration() != null) {
            video.setDuration(updateDTO.getDuration());
        }

        if (updateDTO.getIsActive() != null) {
            video.setIsActive(updateDTO.getIsActive());
        }

        if (updateDTO.getDisplayOrder() != null) {
            video.setDisplayOrder(updateDTO.getDisplayOrder());
        }

        YouTubeVideo updatedVideo = youTubeVideoRepository.save(video);
        log.info("YouTube video updated successfully with ID: {}", updatedVideo.getId());

        return mapToDTO(updatedVideo);
    }

    @Override
    public void deleteVideo(Long id) {
        log.info("Deleting YouTube video with ID: {}", id);

        YouTubeVideo video = youTubeVideoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("YouTube video not found with ID: " + id));

        youTubeVideoRepository.delete(video);
        log.info("YouTube video deleted successfully with ID: {}", id);
    }

    @Override
    public YouTubeVideoDTO getVideoById(Long id) {
        log.info("Fetching YouTube video with ID: {}", id);
        YouTubeVideo video = youTubeVideoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("YouTube video not found with ID: " + id));
        return mapToDTO(video);
    }

    @Override
    public Page<YouTubeVideoDTO> getAllVideos(Pageable pageable) {
        log.info("Fetching all YouTube videos with pagination");
        return youTubeVideoRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToDTO);
    }

    @Override
    public List<YouTubeVideoDTO> getAllVideosList() {
        log.info("Fetching all YouTube videos as list");
        return youTubeVideoRepository.findAllOrdered()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<YouTubeVideoDTO> getActiveVideosForWebsite() {
        log.info("Fetching active YouTube videos for website (latest 6)");
        return youTubeVideoRepository.findTop6ByIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public YouTubeVideoDTO toggleVideoStatus(Long id) {
        log.info("Toggling status for YouTube video with ID: {}", id);

        YouTubeVideo video = youTubeVideoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("YouTube video not found with ID: " + id));

        video.setIsActive(!video.getIsActive());
        YouTubeVideo updatedVideo = youTubeVideoRepository.save(video);

        log.info("YouTube video status toggled to: {} for ID: {}", updatedVideo.getIsActive(), id);
        return mapToDTO(updatedVideo);
    }

    @Override
    public YouTubeVideoDTO updateVideoOrder(Long id, Integer displayOrder) {
        log.info("Updating display order for YouTube video with ID: {} to {}", id, displayOrder);

        YouTubeVideo video = youTubeVideoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("YouTube video not found with ID: " + id));

        video.setDisplayOrder(displayOrder);
        YouTubeVideo updatedVideo = youTubeVideoRepository.save(video);

        log.info("Display order updated for YouTube video ID: {}", id);
        return mapToDTO(updatedVideo);
    }

    private String cleanYouTubeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // If it's already an embed URL, return as is
        if (url.contains("youtube.com/embed/")) {
            return url;
        }

        // Extract video ID from various YouTube URL formats
        String videoId = null;

        // Standard youtube.com/watch?v=VIDEO_ID
        if (url.contains("youtube.com/watch")) {
            String[] params = url.split("[?&]");
            for (String param : params) {
                if (param.startsWith("v=")) {
                    videoId = param.substring(2);
                    break;
                }
            }
        }
        // youtu.be/VIDEO_ID format
        else if (url.contains("youtu.be/")) {
            String[] parts = url.split("youtu.be/");
            if (parts.length > 1) {
                videoId = parts[1].split("[?]")[0];
            }
        }
        // Already has embed format but different parameters
        else if (url.contains("youtube.com/embed/")) {
            return url;
        }

        if (videoId != null && !videoId.isEmpty()) {
            return "https://www.youtube.com/embed/" + videoId;
        }

        // Return original if we couldn't parse it
        return url;
    }

    private YouTubeVideoDTO mapToDTO(YouTubeVideo video) {
        return YouTubeVideoDTO.builder()
                .id(video.getId())
                .title(video.getTitle())
                .iframeUrl(video.getIframeUrl())
                .description(video.getDescription())
                .duration(video.getDuration())
                .isActive(video.getIsActive())
                .displayOrder(video.getDisplayOrder())
                .createdAt(video.getCreatedAt() != null ? video.getCreatedAt().format(DATE_FORMATTER) : null)
                .updatedAt(video.getUpdatedAt() != null ? video.getUpdatedAt().format(DATE_FORMATTER) : null)
                .build();
    }
}