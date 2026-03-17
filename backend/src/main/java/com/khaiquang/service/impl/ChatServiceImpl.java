package com.khaiquang.service.impl;

import com.khaiquang.entity.Trip;
import com.khaiquang.repository.TripRepository;
import com.khaiquang.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final TripRepository tripRepository;

    @Value("classpath:/prompts/travelk-chatbot-instruction.st")
    private Resource systemInstructions;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder, TripRepository tripRepository) {
        this.chatClient = chatClientBuilder.build();
        this.tripRepository = tripRepository;
    }

    @Override
    public String call(String message) {
        // RAG: Tìm kiếm dữ liệu liên quan
        // Ở đây ta tìm kiếm đơn giản: Nếu câu hỏi chứa tên địa điểm, ta query DB
        // Nếu không, ta lấy 5 chuyến xe sắp tới làm context mặc định
        
        List<Trip> trips;
        // Logic tìm kiếm đơn giản (có thể nâng cấp bằng Vector DB sau này)
        if (message.toLowerCase().contains("hà nội")) {
            trips = tripRepository.searchByKeyword("Hà Nội");
        } else if (message.toLowerCase().contains("đà nẵng")) {
            trips = tripRepository.searchByKeyword("Đà Nẵng");
        } else if (message.toLowerCase().contains("sài gòn") || message.toLowerCase().contains("hồ chí minh")) {
            trips = tripRepository.searchByKeyword("Hồ Chí Minh");
        } else {
            // Mặc định lấy tất cả (hoặc top 10) nếu không rõ
            trips = tripRepository.findAll(); 
        }
        
        // Format dữ liệu thành chuỗi text để AI đọc
        String tripData = trips.stream()
                .map(t -> String.format("- Chuyến %d: %s đi %s, Ngày %s lúc %s, Giá: %s VND, Xe: %s",
                        t.getId(), t.getOrigin(), t.getDestination(), 
                        t.getDepartureDate(), t.getDepartureTime(), 
                        t.getPrice(), t.getBus().getBusType()))
                .collect(Collectors.joining("\n"));

    try {
        return chatClient.prompt()
                .system(s -> s.text(systemInstructions).param("tripData", tripData))
                .user(message)
                .call()
                .content();
    }catch(Exception e){
        return "Hệ thống lỗi, xin vui lòng thử lại sau ít phút!";
    }


    }
}
