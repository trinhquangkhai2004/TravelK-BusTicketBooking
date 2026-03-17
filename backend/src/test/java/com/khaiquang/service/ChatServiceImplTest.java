package com.khaiquang.service;

import com.khaiquang.entity.Bus;
import com.khaiquang.entity.Trip;
import com.khaiquang.repository.TripRepository;
import com.khaiquang.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceImplTest {
    @Mock
    private TripRepository tripRepository;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    // Sử dụng RETURNS_DEEP_STUBS để mock mượt mà chuỗi fluent API của Spring AI
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        // Giả lập Builder trả về ChatClient đã được mock của chúng ta
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Khởi tạo Service cần test
        chatService = new ChatServiceImpl(chatClientBuilder, tripRepository);
    }

    @Test
    void call_ShouldReturnBotResponse_WhenUserAsksAboutDaNang() {
        // 1. ARRANGE (Chuẩn bị dữ liệu)
        String userMessage = "Có chuyến xe nào đi Đà Nẵng hôm nay không?";
        String expectedAiResponse = "Hiện tại có 1 chuyến đi Đà Nẵng vào lúc 20:00, giá 400000 VND.";

        Bus mockBus = new Bus();
        mockBus.setBusType("Limousine 34 phòng");

        Trip trip = new Trip();
        trip.setId(1L);
        trip.setOrigin("Hà Nội");
        trip.setDestination("Đà Nẵng");
        trip.setDepartureDate(LocalDate.parse("2026-03-16"));
        trip.setDepartureTime(LocalTime.parse("20:00"));
        trip.setPrice(BigDecimal.valueOf(400000.0));
        trip.setBus(mockBus);

        // Giả lập Repository trả về danh sách chuyến xe khi tìm kiếm "Đà Nẵng"
        when(tripRepository.searchByKeyword("Đà Nẵng")).thenReturn(List.of(trip));

        // Giả lập chuỗi gọi ChatClient trả về kết quả mong muốn
        when(chatClient.prompt()
                .system(any(Consumer.class))
                .user(anyString())
                .call()
                .content()).thenReturn(expectedAiResponse);

        // 2. ACT (Thực thi)
        String actualResponse = chatService.call(userMessage);

        // 3. ASSERT (Kiểm tra kết quả)
        assertEquals(expectedAiResponse, actualResponse, "Phản hồi của bot không khớp với kỳ vọng");

        // Xác minh rằng repository thực sự đã được gọi với từ khóa "Đà Nẵng" chính xác 1 lần
        verify(tripRepository, times(1)).searchByKeyword("Đà Nẵng");

        // Xác minh các nhánh khác KHÔNG bị gọi nhầm
        verify(tripRepository, never()).searchByKeyword("Hà Nội");
        verify(tripRepository, never()).findAll();
    }

    @Test
    void call_ShouldReturnBotResponse_WhenUserAsksGeneralQuestion() {
        // 1. ARRANGE
        String userMessage = "Cho tôi xem các chuyến xe sắp tới";
        String expectedAiResponse = "Đây là danh sách các chuyến xe hiện có...";

        // Giả lập trả về list rỗng cho nhánh mặc định để code đơn giản
        when(tripRepository.findAll()).thenReturn(List.of());

        when(chatClient.prompt()
                .system(any(Consumer.class))
                .user(anyString())
                .call()
                .content()).thenReturn(expectedAiResponse);

        // 2. ACT
        String actualResponse = chatService.call(userMessage);

        // 3. ASSERT
        assertEquals(expectedAiResponse, actualResponse);
        verify(tripRepository, times(1)).findAll();
        verify(tripRepository, never()).searchByKeyword(anyString());
    }
}
