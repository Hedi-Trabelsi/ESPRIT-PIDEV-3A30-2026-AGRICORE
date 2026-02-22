package services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {
    public static void envoyerEmailTache(String destinataire,String descriptionAgri, String descriptionTechnicien, String date, String cout) {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        // TES INFOS ICI
        String monEmail = "mrabetzeineb1@gmail.com";
        String monPassword = "";

        Session session = Session.getInstance(prop, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(monEmail, monPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(monEmail, "AgriCore Support"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject("AgriCore : Details de votre intervention");

            // Le corps du mail avec la description du technicien
            String corpsEmail = "Bonjour,\n\n" +
                    "Pour votre maintenance declaree concernant : \"" + descriptionAgri + "\",\n" +
                    "nous vous informons que la planification est fixee pour le " + date + ".\n\n" +
                    "Note du technicien :\n" +
                    descriptionTechnicien + "\n\n" + // C'est ici qu'on ajoute l'explication technique
                    "Details financiers :\n" +
                    "- Cout estime : " + cout + " DT\n\n" +
                    "L'equipe AgriCore vous remercie pour votre confiance !";

            message.setText(corpsEmail);

            Transport.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}