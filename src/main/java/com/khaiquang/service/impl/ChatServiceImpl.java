package com.khaiquang.service.impl;

import com.khaiquang.entity.Trip;
import com.khaiquang.repository.TripRepository;
import com.khaiquang.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final TripRepository tripRepository;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder, TripRepository tripRepository) {
        this.chatClient = chatClientBuilder.build();
        this.tripRepository = tripRepository;
    }

    @Override
    public String call(String message) {
        // 1. RAG: Tìm kiếm dữ liệu liên quan
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

        // 2. Tạo Prompt
        String prompt = String.format("""
                Bạn là trợ lý ảo của hệ thống đặt vé xe TravelK.
                Dưới đây là danh sách các chuyến xe hiện có trong hệ thống:
                %s
                
                Hãy trả lời câu hỏi của khách hàng dựa trên dữ liệu trên.
                Nếu khách hỏi về chuyến đi không có trong danh sách, hãy xin lỗi và bảo họ thử tìm ngày khác.
                Trả lời ngắn gọn, lịch sự, và cung cấp thông tin giá vé, giờ chạy nếu có.
                
                Câu hỏi của khách: "%s"
                """, tripData, message);

        // 3. Gọi AI
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
