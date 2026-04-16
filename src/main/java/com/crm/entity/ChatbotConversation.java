// ChatbotConversation.java
package com.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String botResponse;

    @Column(nullable = false)
    private String messageType;

    @Column(length = 500)
    private String collectedName;

    @Column(length = 100)
    private String collectedEmail;

    @Column(length = 20)
    private String collectedPhone;

    @Column(length = 50)
    private String collectedService;

    @Column(columnDefinition = "TEXT")
    private String collectedMessage;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isComplete = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leadCreated = false;

    private Long createdLeadId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}