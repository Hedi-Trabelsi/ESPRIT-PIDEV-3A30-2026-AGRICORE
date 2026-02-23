package services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

public class EmailService {

    // Replace these with your Gmail credentials
    private static final String FROM_EMAIL = "heditrabelsi416@gmail.com"; // Your Gmail
    private static final String PASSWORD = "wbkl qbld xxpq wjqb"; // Your Gmail App Password

    /**
     * Generate a random 6-digit verification code
     */
    public static String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }

    /**
     * Send verification code to user's email using Jakarta Mail
     */
    public static boolean sendLoginCode(String toEmail, String code, String userName) {
        // Gmail SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // Create session with authentication (Jakarta Mail)
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });

        try {
            // Create email message (Jakarta Mail)
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Your Login Code - AGRICOR");

            // Email content with HTML formatting
            String emailContent =
                    "<html>" +
                            "<body style='font-family: Arial, sans-serif; margin: 0; padding: 0;'>" +
                            "<div style='background-color: #1b5e20; padding: 30px; text-align: center;'>" +
                            "<h1 style='color: white; margin: 0; font-size: 32px;'>🌱 AGRICOR</h1>" +
                            "</div>" +
                            "<div style='padding: 30px; background-color: #f5f5f5;'>" +
                            "<h2 style='color: #1b5e20; margin-top: 0;'>Hello " + userName + ",</h2>" +
                            "<p style='font-size: 16px; color: #333;'>You requested to log in to your AGRICOR account. Use the verification code below:</p>" +
                            "<div style='background-color: white; padding: 20px; text-align: center; margin: 25px 0; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);'>" +
                            "<h1 style='color: #1b5e20; font-size: 48px; letter-spacing: 8px; margin: 0;'>" + code + "</h1>" +
                            "</div>" +
                            "<p style='font-size: 14px; color: #666;'>This code will expire in <strong>10 minutes</strong>.</p>" +
                            "<p style='font-size: 14px; color: #666;'>If you didn't request this, please ignore this email.</p>" +
                            "<hr style='border: 1px solid #ddd; margin: 25px 0;'>" +
                            "<p style='color: #666; font-size: 12px; text-align: center;'>© 2026 AGRICOR - Smart Agriculture</p>" +
                            "</div>" +
                            "</body>" +
                            "</html>";

            message.setContent(emailContent, "text/html");

            // Send email (Jakarta Mail)
            Transport.send(message);

            System.out.println("Login code sent to: " + toEmail);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send welcome email to new users
     */
    public static boolean sendWelcomeEmail(String toEmail, String userName) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Welcome to AGRICOR!");

            String emailContent =
                    "<html>" +
                            "<body style='font-family: Arial, sans-serif; margin: 0; padding: 0;'>" +
                            "<div style='background-color: #1b5e20; padding: 30px; text-align: center;'>" +
                            "<h1 style='color: white; margin: 0; font-size: 32px;'>🌱 AGRICOR</h1>" +
                            "</div>" +
                            "<div style='padding: 30px; background-color: #f5f5f5;'>" +
                            "<h2 style='color: #1b5e20; margin-top: 0;'>Welcome " + userName + "!</h2>" +
                            "<p style='font-size: 16px; color: #333;'>Thank you for joining AGRICOR - Smart Agriculture platform.</p>" +
                            "<p style='font-size: 14px; color: #666;'>You can now log in using your email and password, or use the face recognition feature.</p>" +
                            "<hr style='border: 1px solid #ddd; margin: 25px 0;'>" +
                            "<p style='color: #666; font-size: 12px; text-align: center;'>© 2026 AGRICOR - Smart Agriculture</p>" +
                            "</div>" +
                            "</body>" +
                            "</html>";

            message.setContent(emailContent, "text/html");
            Transport.send(message);

            System.out.println("Welcome email sent to: " + toEmail);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}