package com.khaiquang.controller;

import com.khaiquang.dto.request.ChatRequestDto;
import com.khaiquang.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequestDto request){
        String message = request.message();
        String response = chatService.call(message);
        return new ResponseEntity<>(Map.of("response", response), HttpStatus.OK);
    }

}
