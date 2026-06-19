package com.kaif.chatapp.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@mychatsweb.tech}")
    private String fromAddress;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        send(toEmail, "Reset your ChatApp password",
                "Click the link below to reset your password (expires in 30 minutes):\n\n" + resetLink
                        + "\n\nIf you did not request this, ignore this email.");
    }

    public void sendVerificationEmail(String toEmail, String verifyLink) {
        send(toEmail, "Verify your ChatApp email",
                "Welcome to ChatApp! Verify your email by clicking:\n\n" + verifyLink
                        + "\n\nThis link expires in 24 hours.");
    }

    private void send(String to, String subject, String body) {
        if (smtpHost == null || smtpHost.isBlank() || "localhost".equals(smtpHost)) {
            log.warn("SMTP not configured — email not sent to {} (subject: {})", to, subject);
            log.debug("Email body: {}", body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Email sent to {}", to);
    }
}
