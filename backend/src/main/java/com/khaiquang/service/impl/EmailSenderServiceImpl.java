package com.khaiquang.service.impl;

import com.khaiquang.service.EmailSenderService;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailSenderServiceImpl implements EmailSenderService {
    private final Configuration freemarkerConfiguration;
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendSeatsInformation(String toEmail, Map<String, Object> attributes) {
        String text = getEmailContent("seats-info.ftlh", attributes);
        sendEmail(toEmail, "[TravelK] Xác nhận đặt vé thành công", text);
    }

    @Override
    public void sendResetPasswordEmail(String toEmail, String userName, String resetLink) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userName", userName);
        attributes.put("resetLink", resetLink);
        
        String text = getEmailContent("reset-password.ftlh", attributes);
        sendEmail(toEmail, "[TravelK] Yêu cầu đặt lại mật khẩu", text);
    }

    private void sendEmail(String toEmail, String subject, String text) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(text, true);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email to " + toEmail, e);
        }
    }

    private String getEmailContent(String template, Map<String, Object> attributes) {
        try {
            StringWriter stringWriter = new StringWriter();
            freemarkerConfiguration.getTemplate(template).process(attributes, stringWriter);
            return stringWriter.getBuffer().toString();
        } catch (TemplateException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
