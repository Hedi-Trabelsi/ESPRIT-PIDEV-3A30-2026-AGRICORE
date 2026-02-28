package services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
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
     * Get mail session with common configuration
     */
    private static Session getMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
    }

    // ==================== LOGIN CODE EMAIL ====================

    /**
     * Send verification code to user's email
     */
    public static boolean sendLoginCode(String toEmail, String code, String userName) {
        try {
            Session session = getMailSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "AGRICOR"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Your Login Code - AGRICOR");

            String emailContent = buildLoginCodeEmail(userName, code);
            message.setContent(emailContent, "text/html");

            Transport.send(message);
            System.out.println("Login code sent to: " + toEmail);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String buildLoginCodeEmail(String userName, String code) {
        return "<html>" +
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
    }

    // ==================== WELCOME EMAIL ====================

    /**
     * Send welcome email to new users
     */
    public static boolean sendWelcomeEmail(String toEmail, String userName) {
        try {
            Session session = getMailSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "AGRICOR"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Welcome to AGRICOR!");

            String emailContent = buildWelcomeEmail(userName);
            message.setContent(emailContent, "text/html");

            Transport.send(message);
            System.out.println("Welcome email sent to: " + toEmail);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String buildWelcomeEmail(String userName) {
        return "<html>" +
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
    }

    // ==================== TASK/INTERVENTION EMAIL WITH PDF ATTACHMENT ====================

    /**
     * Send task/intervention email with PDF attachment
     */
    public static boolean envoyerEmailTache(String destinataire, String descriptionAgri,
                                            String descriptionTechnicien, String date,
                                            String cout, String filename) {
        try {
            Session session = getMailSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "AgriCore Support"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject("AgriCore : Details de votre intervention");

            // 1. Create email body (Text)
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            String corpsEmail = buildTaskEmailBody(descriptionAgri, descriptionTechnicien, date, cout);
            messageBodyPart.setText(corpsEmail);

            // 2. Create PDF attachment
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(new File(filename));

            // 3. Combine text and PDF
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentPart);

            // 4. Set content
            message.setContent(multipart);

            // Send
            Transport.send(message);
            System.out.println("Email envoyé avec succès avec le PDF !");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String buildTaskEmailBody(String descriptionAgri, String descriptionTechnicien,
                                             String date, String cout) {
        return "Bonjour,\n\n" +
                "Pour votre maintenance déclarée concernant : \"" + descriptionAgri + "\",\n" +
                "nous vous informons que la planification est fixée pour le " + date + ".\n\n" +
                "Note du technicien :\n" +
                descriptionTechnicien + "\n\n" +
                "Détails financiers :\n" +
                "- Coût estimé : " + cout + " DT\n\n" +
                "Veuillez trouver ci-joint le rapport officiel de cette intervention.\n\n" +
                "L'équipe AgriCore vous remercie pour votre confiance !";
    }

    // ==================== GENERIC EMAIL SENDER ====================

    /**
     * Send a simple text email
     */
    public static boolean sendSimpleEmail(String toEmail, String subject, String content) {
        try {
            Session session = getMailSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "AGRICOR"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);
            System.out.println("Email sent to: " + toEmail);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send an HTML email
     */
    public static boolean sendHTMLEmail(String toEmail, String subject, String htmlContent) {
        try {
            Session session = getMailSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "AGRICOR"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html");

            Transport.send(message);
            System.out.println("HTML Email sent to: " + toEmail);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}