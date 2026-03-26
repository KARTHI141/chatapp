package com.chatapp.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("ChatApp - Verify Your Email");
            message.setText(
                "Welcome to ChatApp!\n\n" +
                "Your verification code is: " + otp + "\n\n" +
                "This code expires in 5 minutes.\n\n" +
                "If you didn't request this, please ignore this email."
            );

            mailSender.send(message);
            log.info("OTP email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
        }
    }
}
