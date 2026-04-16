// ChatbotConversationRepository.java
package com.crm.repository;

import com.crm.entity.ChatbotConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatbotConversationRepository extends JpaRepository<ChatbotConversation, Long> {

    Optional<ChatbotConversation> findTopBySessionIdOrderByCreatedAtDesc(String sessionId);

    List<ChatbotConversation> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    Optional<ChatbotConversation> findBySessionIdAndIsCompleteFalse(String sessionId);

    boolean existsBySessionIdAndIsCompleteFalse(String sessionId);
}