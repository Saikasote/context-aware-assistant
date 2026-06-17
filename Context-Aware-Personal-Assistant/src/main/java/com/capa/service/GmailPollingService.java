package com.capa.service;

import com.capa.model.OAuthToken;
import com.capa.repository.OAuthTokenRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GmailPollingService {

    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;
    private final OAuthTokenRepository tokenRepository;

    private Gmail getGmailClient(String accessToken) {
        Credential credential = new GoogleCredential().setAccessToken(accessToken);
        return new Gmail.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Context-Aware-Personal-Assistant")
                .build();
    }

    public List<String> fetchRecentEmailSnippets(String userEmail) {
        try {
            // Gracefully find the token instead of throwing a generic RuntimeException crash
            OAuthToken token = tokenRepository.findByUserEmail(userEmail)
                    .orElse(null);

            if (token == null || token.getAccessToken() == null) {
                System.out.println("⚠️ Gmail API warning: No OAuth token found in database for: " + userEmail);
                return Collections.emptyList();
            }

            Gmail client = getGmailClient(token.getAccessToken());

            ListMessagesResponse response = client.users().messages().list("me")
                    .setMaxResults(5L)
                    .setQ("is:unread category:primary")
                    .execute();

            if (response.getMessages() == null) {
                return Collections.emptyList();
            }

            List<String> snippets = new ArrayList<>();
            for (Message msg : response.getMessages()) {
                // Fetch the full message payload to extract the text snippet body safely
                Message fullMsg = client.users().messages().get("me", msg.getId()).execute();
                if (fullMsg.getSnippet() != null) {
                    snippets.add(fullMsg.getSnippet());
                }
            }
            return snippets;
        } catch (IOException e) {
            System.err.println("❌ Gmail Primary Inbox Fetch failure: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}