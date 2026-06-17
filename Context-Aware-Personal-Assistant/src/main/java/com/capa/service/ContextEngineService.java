package com.capa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContextEngineService {

    private final ChatClient.Builder chatClientBuilder;

    /**
     * Takes an email snippet or free-form user sentence and asks Ollama to extract an actionable task.
     * * @param rawText The raw text content (e.g., "Meeting with professor tomorrow at 3 PM about project documentation")
     * @return Structured plain text instruction or format summarizing Title, Description, and Due Date.
     */
    public String extractTaskFromContext(String rawText) {
        ChatClient chatClient = chatClientBuilder.build();

        String systemPrompt = """
                You are a precise context-aware task extraction engine. 
                Your job is to read the provided raw text (which could be an email snippet or a voice/text note) 
                and extract a clear, actionable task.
                
                You must respond STRICTLY using the format below. Do not include any greeting, pleasantries, or additional commentary.
                
                FORMAT:
                TITLE: [A short summary task name]
                DESCRIPTION: [Details of what needs to be done based on text]
                DAYS_FROM_NOW: [An integer indicating when this is due. If today/unstated, output 0. If tomorrow, 1. Next week, 7, etc.]
                """;

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user("Extract a task from this content: " + rawText)
                    .call()
                    .content();
        } catch (Exception e) {
            return "TITLE: Follow-up required\nDESCRIPTION: " + rawText + "\nDAYS_FROM_NOW: 1";
        }
    }
}