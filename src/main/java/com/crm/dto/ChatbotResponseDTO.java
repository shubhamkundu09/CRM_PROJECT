// ChatbotResponseDTO.java
package com.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponseDTO {

    private String response;

    private String sessionId;

    private String messageType;

    private Boolean isComplete;

    private Boolean leadCreated;

    private String collectedName;

    private String collectedEmail;

    private String collectedPhone;
}