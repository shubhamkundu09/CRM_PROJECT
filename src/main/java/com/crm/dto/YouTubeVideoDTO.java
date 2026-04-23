package com.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeVideoDTO {
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 200, message = "Title must be between 2 and 200 characters")
    private String title;

    @NotBlank(message = "YouTube iframe URL is required")
    private String iframeUrl;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private String duration;

    private Boolean isActive;

    private Integer displayOrder;

    private String createdAt;

    private String updatedAt;
}