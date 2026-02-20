package com.khaiquang.service;

import java.util.Map;

public interface EmailSenderService {
    void sendSeatsInformation(String toEmail, Map<String,Object> attributes);
    void sendResetPasswordEmail(String toEmail, String userName, String resetLink);
}
