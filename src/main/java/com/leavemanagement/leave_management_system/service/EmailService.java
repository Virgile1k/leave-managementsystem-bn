package com.leavemanagement.leave_management_system.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    public void sendSimpleMessage(String to, String subject, String text) {
        if (!emailEnabled) {
            log.info("Email sending is disabled. Would have sent to: {}, subject: {}", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }

    public void sendHtmlMessage(String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            log.info("Email sending is disabled. Would have sent HTML email to: {}, subject: {}", to, subject);
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
        }
    }

    public String generateLeaveRequestHtml(String recipientName, String subject, String message,
                                           boolean showButtons, String approveUrl, String rejectUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head>")
                .append("    <meta charset=\"UTF-8\">")
                .append("    <title>").append(subject).append("</title>")
                .append("    <style>")
                .append("        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }")
                .append("        .container { max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }")
                .append("        .header { background-color: #f5f5f5; padding: 10px; text-align: center; border-radius: 5px 5px 0 0; }")
                .append("        .content { padding: 20px; }")
                .append("        .footer { background-color: #f5f5f5; padding: 10px; text-align: center; border-radius: 0 0 5px 5px; font-size: 12px; }")
                .append("        .button { display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px; margin: 10px 5px; }")
                .append("        .reject-button { background-color: #f44336; }")
                .append("    </style>")
                .append("</head>")
                .append("<body>")
                .append("    <div class=\"container\">")
                .append("        <div class=\"header\">")
                .append("            <h2>Leave Management System</h2>")
                .append("        </div>")
                .append("        <div class=\"content\">")
                .append("            <h3>").append(subject).append("</h3>")
                .append("            <p>Hello ").append(recipientName).append(",</p>")
                .append("            <p>").append(message).append("</p>");

        if (showButtons) {
            html.append("            <div>")
                    .append("                <a href=\"").append(approveUrl).append("\" class=\"button\">Approve</a>")
                    .append("                <a href=\"").append(rejectUrl).append("\" class=\"button reject-button\">Reject</a>")
                    .append("            </div>");
        }

        html.append("            <p>Thank you,<br>")
                .append("            Leave Management System</p>")
                .append("        </div>")
                .append("        <div class=\"footer\">")
                .append("            <p>This is an automated message, please do not reply directly to this email.</p>")
                .append("        </div>")
                .append("    </div>")
                .append("</body>")
                .append("</html>");

        return html.toString();
    }

    public String generateLeaveStatusHtml(String recipientName, String subject, String message) {
        return generateLeaveRequestHtml(recipientName, subject, message, false, null, null);
    }
}