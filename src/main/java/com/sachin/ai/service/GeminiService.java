package com.sachin.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class GeminiService {

    @Autowired
    PortfolioData  portfolioData;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final String apiKey;
    private final String model;

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    public GeminiService(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model
    ) {

        this.apiKey = apiKey;
        this.model = model;

        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();

        System.out.println("================================");
        System.out.println("GeminiService initialized");
        System.out.println("Model: " + model);
        System.out.println("API Key loaded: " +
                (apiKey != null && !apiKey.isBlank()));
        System.out.println("================================");
    }

    public String askAI(String userMessage) throws Exception {

        String systemPrompt = """
                You are Sachin Kumar's personal AI portfolio assistant.

                Answer questions about Sachin using ONLY the
                portfolio information provided below.

                Never invent information.

                If the information is not available, say:
                "I don't have that information in Sachin's portfolio."

                Be professional, friendly and concise.

                Do not use Markdown.
                Do not use unnecessary headings.
                Do not use Markdown links.

                ================================
                SACHIN'S PORTFOLIO INFORMATION
                ================================

                %s

                ================================
                END PORTFOLIO INFORMATION
                ================================
                """.formatted(
                portfolioData.getProfile()
        );

        String fullPrompt =
                systemPrompt
                        + "\n\nUSER QUESTION:\n"
                        + userMessage;

        String requestBody = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": %s
                        }
                      ]
                    }
                  ]
                }
                """.formatted(
                objectMapper.writeValueAsString(fullPrompt)
        );

        String endpoint =
                GEMINI_BASE_URL
                        + model
                        + ":generateContent";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .header(
                                "x-goog-api-key",
                                apiKey
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(requestBody)
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        System.out.println(
                "Gemini HTTP Status: "
                        + response.statusCode()
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            System.out.println(
                    "Gemini API Error:"
            );

            System.out.println(
                    response.body()
            );

            throw new RuntimeException(
                    "Gemini API request failed: "
                            + response.body()
            );
        }

        JsonNode root =
                objectMapper.readTree(
                        response.body()
                );

        JsonNode candidates =
                root.path("candidates");

        if (!candidates.isArray()
                || candidates.isEmpty()) {

            throw new RuntimeException(
                    "Gemini returned no candidates: "
                            + response.body()
            );
        }

        JsonNode text =
                candidates
                        .get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text");

        if (text.isMissingNode()
                || text.isNull()) {

            throw new RuntimeException(
                    "Gemini returned no text: "
                            + response.body()
            );
        }

        return text.asText();
    }
}