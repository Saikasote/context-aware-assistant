package com.capa.controller;

import com.capa.service.GmailPollingService;
import com.capa.service.GoogleCalendarService;
import com.google.api.services.calendar.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final GoogleCalendarService calendarService;
    private final GmailPollingService gmailPollingService;

    @GetMapping("/dashboard-context")
    public ResponseEntity<Map<String, Object>> getContextSummary(@AuthenticationPrincipal String email) {
        Map<String, Object> contextData = new HashMap<>();

        List<Event> googleCalendarEvents = calendarService.getUpcomingEvents(email);
        List<String> recentEmailSnippets = gmailPollingService.fetchRecentEmailSnippets(email);

        contextData.put("calendarEvents", googleCalendarEvents);
        contextData.put("unreadEmailSnippets", recentEmailSnippets);
        contextData.put("timestamp", java.time.LocalDateTime.now());

        return ResponseEntity.ok(contextData);
    }
}