// ChatbotController.java
package com.crm.controller;

import com.crm.dto.ApiResponse;
import com.crm.dto.ChatbotMessageDTO;
import com.crm.dto.ChatbotResponseDTO;
import com.crm.service.ChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatbotResponseDTO>> processMessage(
            @Valid @RequestBody ChatbotMessageDTO messageDTO,
            HttpServletRequest request) {
        log.info("Chatbot message received for session: {}", messageDTO.getSessionId());
        ChatbotResponseDTO response = chatbotService.processMessage(messageDTO);
        return ResponseEntity.ok(ApiResponse.success(response, "Message processed successfully", request.getRequestURI()));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<ChatbotResponseDTO>> getConversationHistory(
            @RequestParam String sessionId,
            HttpServletRequest request) {
        log.info("Fetching conversation history for session: {}", sessionId);
        ChatbotResponseDTO response = chatbotService.getConversationHistory(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response, "History retrieved successfully", request.getRequestURI()));
    }
}