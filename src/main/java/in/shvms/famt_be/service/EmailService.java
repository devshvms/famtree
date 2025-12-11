package in.shvms.famt_be.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Email Service for sending OTP and verification emails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.name:Family Tree App}")
    private String appName;
    
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * Generate a 6-digit OTP
     */
    public String generateOTP() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
    
    /**
     * Send OTP email for verification
     */
    public void sendOTPEmail(String toEmail, String otp, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(appName + " - Email Verification OTP");
            message.setText(buildOTPEmailBody(otp, userName));
            
            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    /**
     * Send welcome email with temporary PIN
     */
    public void sendWelcomeEmail(String toEmail, String userName, String temporaryPin) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to " + appName);
            message.setText(buildWelcomeEmailBody(userName, temporaryPin, toEmail));
            
            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }
    
    /**
     * Send password reset success notification
     */
    public void sendPasswordResetConfirmation(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(appName + " - Password Updated Successfully");
            message.setText(buildPasswordResetConfirmationBody(userName));
            
            mailSender.send(message);
            log.info("Password reset confirmation sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset confirmation to: {}", toEmail, e);
        }
    }
    
    private String buildOTPEmailBody(String otp, String userName) {
        return String.format("""
            Hello %s,
            
            Your email verification code is: %s
            
            This code will expire in 10 minutes.
            
            If you didn't request this code, please ignore this email.
            
            Best regards,
            %s Team
            """, userName, otp, appName);
    }
    
    private String buildWelcomeEmailBody(String userName, String temporaryPin, String email) {
        return String.format("""
            Hello %s,
            
            Welcome to %s! Your account has been created by your family administrator.
            
            Your login credentials:
            Email: %s
            Temporary PIN: %s
            
            For security reasons, you will need to:
            1. Login with your email and temporary PIN
            2. Verify your email address with an OTP
            3. Set a new secure password
            
            Please login at your earliest convenience to complete the setup.
            
            Best regards,
            %s Team
            """, userName, appName, email, temporaryPin, appName);
    }
    
    private String buildPasswordResetConfirmationBody(String userName) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format("""
            Hello %s,
            
            Your password was successfully updated on %s.
            
            If you didn't make this change, please contact your administrator immediately.
            
            Best regards,
            %s Team
            """, userName, timestamp, appName);
    }
}