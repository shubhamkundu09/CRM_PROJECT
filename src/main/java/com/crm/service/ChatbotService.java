// ChatbotService.java - Fixed version with proper null handling
package com.crm.service;

import com.crm.dto.ChatbotMessageDTO;
import com.crm.dto.ChatbotResponseDTO;
import com.crm.dto.LeadResponseDTO;
import com.crm.dto.WebsiteLeadDTO;
import com.crm.entity.ChatbotConversation;
import com.crm.entity.MainService;
import com.crm.repository.ChatbotConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatbotService {

    private final ChatbotConversationRepository conversationRepository;
    private final WebsiteLeadService websiteLeadService;

    // Service mapping for display names
    private static final Map<String, String> SERVICE_DISPLAY_MAP = new HashMap<>();
    private static final Map<String, String> SERVICE_REVERSE_MAP = new HashMap<>();

    static {
        SERVICE_DISPLAY_MAP.put("LIFE_INSURANCE", "Life Insurance");
        SERVICE_DISPLAY_MAP.put("HEALTH_INSURANCE", "Health Insurance");
        SERVICE_DISPLAY_MAP.put("GENERAL_INSURANCE", "General Insurance");
        SERVICE_DISPLAY_MAP.put("INVESTMENT", "Investment");
        SERVICE_DISPLAY_MAP.put("RETIREMENT_PLANNING", "Retirement Planning");
        SERVICE_DISPLAY_MAP.put("BUSINESS_INSURANCE", "Business Insurance");

        SERVICE_REVERSE_MAP.put("life insurance", "LIFE_INSURANCE");
        SERVICE_REVERSE_MAP.put("health insurance", "HEALTH_INSURANCE");
        SERVICE_REVERSE_MAP.put("general insurance", "GENERAL_INSURANCE");
        SERVICE_REVERSE_MAP.put("investment", "INVESTMENT");
        SERVICE_REVERSE_MAP.put("retirement planning", "RETIREMENT_PLANNING");
        SERVICE_REVERSE_MAP.put("business insurance", "BUSINESS_INSURANCE");
        SERVICE_REVERSE_MAP.put("life", "LIFE_INSURANCE");
        SERVICE_REVERSE_MAP.put("health", "HEALTH_INSURANCE");
        SERVICE_REVERSE_MAP.put("general", "GENERAL_INSURANCE");
        SERVICE_REVERSE_MAP.put("invest", "INVESTMENT");
        SERVICE_REVERSE_MAP.put("retirement", "RETIREMENT_PLANNING");
        SERVICE_REVERSE_MAP.put("business", "BUSINESS_INSURANCE");
    }

    public ChatbotResponseDTO processMessage(ChatbotMessageDTO messageDTO) {
        String sessionId = messageDTO.getSessionId();
        String userMessage = messageDTO.getMessage().trim();

        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = generateSessionId();
        }

        log.info("Processing chatbot message for session: {}, message: {}", sessionId, userMessage);

        // Get or create conversation - without lambda
        Optional<ChatbotConversation> existingConversation = conversationRepository
                .findBySessionIdAndIsCompleteFalse(sessionId);
        ChatbotConversation conversation;

        if (existingConversation.isPresent()) {
            conversation = existingConversation.get();
        } else {
            conversation = createNewConversation(sessionId);
        }

        // Process based on current state
        return processConversation(conversation, userMessage);
    }

    private ChatbotConversation createNewConversation(String sessionId) {
        ChatbotConversation conversation = ChatbotConversation.builder()
                .sessionId(sessionId)
                .messageType("INTRO")
                .isComplete(false)
                .leadCreated(false)
                .userMessage("")  // Will be updated later
                .botResponse("")  // Will be updated later
                .build();
        return conversationRepository.save(conversation);
    }

    private ChatbotResponseDTO processConversation(ChatbotConversation conversation, String userMessage) {
        String currentType = conversation.getMessageType();
        ChatbotResponseDTO response = new ChatbotResponseDTO();
        response.setSessionId(conversation.getSessionId());
        response.setIsComplete(false);
        response.setLeadCreated(false);

        switch (currentType) {
            case "INTRO":
                response.setResponse("👋 Hi there! Welcome to Samriddhi Financial Services! I'm your virtual assistant. 😊\n\n" +
                        "I can help you get a quote for insurance or investment products. Can I please have your name?");
                response.setMessageType("ASKING_NAME");
                conversation.setMessageType("ASKING_NAME");

                // Update conversation with user message and bot response
                conversation.setUserMessage(userMessage);
                conversation.setBotResponse(response.getResponse());
                conversationRepository.save(conversation);
                break;

            case "ASKING_NAME":
                String name = userMessage;
                conversation.setCollectedName(name);
                conversation.setMessageType("ASKING_EMAIL");
                response.setResponse("Nice to meet you, " + name + "! 🎉\n\n" +
                        "Could you please share your email address? I'll send you the quote details there.");
                response.setMessageType("ASKING_EMAIL");

                // Update conversation with user message and bot response
                conversation.setUserMessage(userMessage);
                conversation.setBotResponse(response.getResponse());
                conversationRepository.save(conversation);
                break;

            case "ASKING_EMAIL":
                if (!isValidEmail(userMessage)) {
                    response.setResponse("❌ Please enter a valid email address (e.g., name@example.com).");
                    response.setMessageType("ASKING_EMAIL");

                    // Update conversation with user message and bot response
                    conversation.setUserMessage(userMessage);
                    conversation.setBotResponse(response.getResponse());
                    conversationRepository.save(conversation);
                    return response;
                }
                conversation.setCollectedEmail(userMessage.toLowerCase());
                conversation.setMessageType("ASKING_PHONE");
                response.setResponse("Great! 📧 Now, could you please share your 10-digit mobile number?");
                response.setMessageType("ASKING_PHONE");

                // Update conversation with user message and bot response
                conversation.setUserMessage(userMessage);
                conversation.setBotResponse(response.getResponse());
                conversationRepository.save(conversation);
                break;

            case "ASKING_PHONE":
                if (!isValidPhone(userMessage)) {
                    response.setResponse("❌ Please enter a valid 10-digit mobile number (only numbers).");
                    response.setMessageType("ASKING_PHONE");

                    // Update conversation with user message and bot response
                    conversation.setUserMessage(userMessage);
                    conversation.setBotResponse(response.getResponse());
                    conversationRepository.save(conversation);
                    return response;
                }
                conversation.setCollectedPhone(userMessage);
                conversation.setMessageType("ASKING_SERVICE");
                response.setResponse("Perfect! 📱 Now, what service are you interested in?\n\n" +
                        "Please choose from:\n" +
                        "🏥 • Health Insurance\n" +
                        "❤️ • Life Insurance\n" +
                        "🚗 • General Insurance\n" +
                        "📈 • Investment\n" +
                        "👴 • Retirement Planning\n" +
                        "🏢 • Business Insurance\n\n" +
                        "Just type the name of the service you're interested in.");
                response.setMessageType("ASKING_SERVICE");

                // Update conversation with user message and bot response
                conversation.setUserMessage(userMessage);
                conversation.setBotResponse(response.getResponse());
                conversationRepository.save(conversation);
                break;

            case "ASKING_SERVICE":
                String service = detectService(userMessage);
                if (service == null) {
                    response.setResponse("❌ I didn't recognize that service. Please choose from:\n" +
                            "• Health Insurance\n" +
                            "• Life Insurance\n" +
                            "• General Insurance\n" +
                            "• Investment\n" +
                            "• Retirement Planning\n" +
                            "• Business Insurance");
                    response.setMessageType("ASKING_SERVICE");

                    // Update conversation with user message and bot response
                    conversation.setUserMessage(userMessage);
                    conversation.setBotResponse(response.getResponse());
                    conversationRepository.save(conversation);
                    return response;
                }
                conversation.setCollectedService(service);
                conversation.setMessageType("ASKING_MESSAGE");
                response.setResponse("Excellent choice! " + SERVICE_DISPLAY_MAP.get(service) + " is a great option. 💪\n\n" +
                        "Do you have any specific requirements or questions? (e.g., coverage amount, family details, existing policies)\n\n" +
                        "Type 'skip' if you don't have any specific message.");
                response.setMessageType("ASKING_MESSAGE");

                // Update conversation with user message and bot response
                conversation.setUserMessage(userMessage);
                conversation.setBotResponse(response.getResponse());
                conversationRepository.save(conversation);
                break;

            case "ASKING_MESSAGE":
                String message = userMessage;
                if (!"skip".equalsIgnoreCase(message) && !"no".equalsIgnoreCase(message)) {
                    conversation.setCollectedMessage(message);
                } else {
                    conversation.setCollectedMessage("No specific requirements provided");
                }

                // Create lead
                LeadResponseDTO lead = createLeadFromConversation(conversation);

                if (lead != null) {
                    conversation.setLeadCreated(true);
                    conversation.setCreatedLeadId(lead.getId());
                    conversation.setIsComplete(true);
                    conversation.setMessageType("LEAD_CREATED");

                    response.setResponse("🎉 Thank you, " + conversation.getCollectedName() + "! Your request has been submitted successfully!\n\n" +
                            "✅ Here's what we've received:\n" +
                            "• Name: " + conversation.getCollectedName() + "\n" +
                            "• Email: " + conversation.getCollectedEmail() + "\n" +
                            "• Phone: " + conversation.getCollectedPhone() + "\n" +
                            "• Service: " + SERVICE_DISPLAY_MAP.get(conversation.getCollectedService()) + "\n\n" +
                            "📞 Our team will contact you within 24 hours.\n\n" +
                            "Thank you for choosing Samriddhi Financial Services! 🙏");
                    response.setIsComplete(true);
                    response.setLeadCreated(true);
                    response.setCollectedName(conversation.getCollectedName());
                    response.setCollectedEmail(conversation.getCollectedEmail());
                    response.setCollectedPhone(conversation.getCollectedPhone());

                    // Update conversation with user message and bot response
                    conversation.setUserMessage(userMessage);
                    conversation.setBotResponse(response.getResponse());
                    conversationRepository.save(conversation);
                } else {
                    response.setResponse("❌ Sorry, there was an error submitting your request. Please try again later or call us directly.\n\n" +
                            "You can also fill out the contact form on our website.");
                    response.setMessageType("ERROR");

                    // Update conversation with user message and bot response
                    conversation.setUserMessage(userMessage);
                    conversation.setBotResponse(response.getResponse());
                    conversationRepository.save(conversation);
                }
                break;

            default:
                response.setResponse("👋 Hello! I'm here to help you with insurance and investment quotes.\n\n" +
                        "Would you like to get a quote? Just type 'yes' to start, or fill out the contact form above.");
                response.setMessageType("INTRO");
                conversation.setMessageType("INTRO");

                // Update conversation with user message and bot response
                conversation.setUserMessage(userMessage);
                conversation.setBotResponse(response.getResponse());
                conversationRepository.save(conversation);
        }

        return response;
    }

    private LeadResponseDTO createLeadFromConversation(ChatbotConversation conversation) {
        try {
            WebsiteLeadDTO leadDTO = WebsiteLeadDTO.builder()
                    .name(conversation.getCollectedName())
                    .email(conversation.getCollectedEmail())
                    .phoneNumber(conversation.getCollectedPhone())
                    .interestedService(MainService.valueOf(conversation.getCollectedService()))
                    .remarks("Chatbot Lead: " + conversation.getCollectedMessage())
                    .build();

            return websiteLeadService.submitWebsiteLead(leadDTO);
        } catch (Exception e) {
            log.error("Error creating lead from chatbot conversation: {}", e.getMessage());
            return null;
        }
    }

    private String detectService(String message) {
        String lowerMessage = message.toLowerCase().trim();

        // Direct match
        if (SERVICE_REVERSE_MAP.containsKey(lowerMessage)) {
            return SERVICE_REVERSE_MAP.get(lowerMessage);
        }

        // Partial matches
        if (lowerMessage.contains("life") || lowerMessage.contains("term")) {
            return "LIFE_INSURANCE";
        }
        if (lowerMessage.contains("health") || lowerMessage.contains("medical") || lowerMessage.contains("hospital")) {
            return "HEALTH_INSURANCE";
        }
        if (lowerMessage.contains("general") || lowerMessage.contains("motor") || lowerMessage.contains("car") ||
                lowerMessage.contains("home") || lowerMessage.contains("travel") || lowerMessage.contains("vehicle")) {
            return "GENERAL_INSURANCE";
        }
        if (lowerMessage.contains("invest") || lowerMessage.contains("mutual") || lowerMessage.contains("ulip") ||
                lowerMessage.contains("fund") || lowerMessage.contains("saving")) {
            return "INVESTMENT";
        }
        if (lowerMessage.contains("retirement") || lowerMessage.contains("pension") || lowerMessage.contains("annuity")) {
            return "RETIREMENT_PLANNING";
        }
        if (lowerMessage.contains("business") || lowerMessage.contains("corporate") || lowerMessage.contains("commercial")) {
            return "BUSINESS_INSURANCE";
        }

        return null;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }

    private boolean isValidPhone(String phone) {
        String phoneRegex = "^[0-9]{10}$";
        return phone != null && phone.matches(phoneRegex);
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    public ChatbotResponseDTO getConversationHistory(String sessionId) {
        ChatbotResponseDTO response = new ChatbotResponseDTO();
        response.setSessionId(sessionId);

        ChatbotConversation conversation = conversationRepository.findBySessionIdAndIsCompleteFalse(sessionId).orElse(null);
        if (conversation != null) {
            response.setIsComplete(conversation.getIsComplete());
            response.setLeadCreated(conversation.getLeadCreated());
            response.setCollectedName(conversation.getCollectedName());
            response.setCollectedEmail(conversation.getCollectedEmail());
            response.setCollectedPhone(conversation.getCollectedPhone());
        }

        return response;
    }
}