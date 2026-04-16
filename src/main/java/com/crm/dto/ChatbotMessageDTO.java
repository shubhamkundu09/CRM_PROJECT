// ChatbotMessageDTO.java
package com.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageDTO {

    @NotBlank(message = "Message cannot be empty")
    private String message;

    private String sessionId;

    private String messageType;

    private Boolean isComplete;

    private Boolean leadCreated;
}