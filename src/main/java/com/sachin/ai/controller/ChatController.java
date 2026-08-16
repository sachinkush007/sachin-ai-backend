package com.sachin.ai.controller;

import com.sachin.ai.dto.ChatRequest;
import com.sachin.ai.dto.ChatResponse;
import com.sachin.ai.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "https://sachin-portfolio-ashy.vercel.app"
})
public class ChatController {

    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {

        if (request == null || request.message() == null
                || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(
                            new ChatResponse(
                                    "Please enter a question."
                            )
                    );
        }

        try {

            String answer =
                    geminiService.askAI(
                            request.message()
                    );

            return ResponseEntity.ok(
                    new ChatResponse(answer)
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            new ChatResponse(
                                    "ERROR: " + e.getMessage()
                            )
                    );
        }
    }
}
