package com.capa.service;

import com.capa.model.OAuthToken;
import com.capa.repository.OAuthTokenRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;
    private final OAuthTokenRepository tokenRepository;

    private Calendar getCalendarClient(String accessToken) {
        Credential credential = new GoogleCredential().setAccessToken(accessToken);
        return new Calendar.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Context-Aware-Personal-Assistant")
                .build();
    }

    public List<Event> getUpcomingEvents(String userEmail) {
        try {
            Optional<OAuthToken> tokenOpt = tokenRepository.findByUserEmail(userEmail);
            if (tokenOpt.isEmpty() || tokenOpt.get().getAccessToken() == null) {
                System.out.println("⚠️ Calendar API warning: No OAuth token found for user: " + userEmail);
                return Collections.emptyList();
            }

            Calendar client = getCalendarClient(tokenOpt.get().getAccessToken());


            com.google.api.client.util.DateTime now = new com.google.api.client.util.DateTime(System.currentTimeMillis());

            // Execute the query cleanly with both valid paired options intact
            Events events = client.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            return events.getItems() != null ? events.getItems() : Collections.emptyList();
        } catch (IOException e) {
            System.err.println("❌ Google Calendar API Fetch failure: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}