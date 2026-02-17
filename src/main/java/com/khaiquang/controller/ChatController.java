package com.khaiquang.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatModel chatModel;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        
        // ChatModel luôn có phương thức call(String) hoặc call(Prompt)
        String response = chatModel.call(message);

        return ResponseEntity.ok(Map.of("response", response));
    }
}
