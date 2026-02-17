package com.khaiquang.consumer;

import com.khaiquang.dto.message.OrderEvent;
import com.khaiquang.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class OrderConsumer {
    private final EmailSenderService emailSenderService;
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderConsumer.class);

    @RabbitListener(queues = "${rabbitmq.queue.order.name}")
    public void consume(OrderEvent event) {
        LOGGER.info("Receiving order event: {}", event);

        try {
            String infoSeats = String.join(", ", event.getContent().getSeats());

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("seat", infoSeats);
            attributes.put("customerName", event.getContent().getCustomerName());
            attributes.put("tripName", event.getContent().getTripName());
            attributes.put("ticketCode", event.getContent().getTickerCode());
            attributes.put("bookingDate", event.getContent().getBookingDate());
            attributes.put("departureDate", event.getContent().getDepartureDate());
            attributes.put("departureTime", event.getContent().getDepartureTime());
            attributes.put("origin", event.getContent().getOrigin());
            attributes.put("destination", event.getContent().getDestination());
            attributes.put("busNumber", event.getContent().getBusNumber());
            attributes.put("busType", event.getContent().getBusType());
            attributes.put("totalPrice", event.getContent().getTotalPrice());
            attributes.put("paymentStatus", event.getContent().getPaymentStatus());


            emailSenderService.sendSeatsInformation(event.getContent().getDesEmail(), attributes);
            LOGGER.info("Email sent successfully to {}", event.getContent().getDesEmail());

        } catch (Exception e) {
            LOGGER.error("Failed to process order event: {}", event, e);
            throw e;
        }
    }
}
