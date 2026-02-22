package services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {
    public static void envoyerEmailTache(String destinataire,String descriptionAgri, String descriptionTechnicien, String date, String cout, String filename) {
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

            // 1. Création du corps du mail (Texte)
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            String corpsEmail = "Bonjour,\n\n" +
                    "Pour votre maintenance declaree concernant : \"" + descriptionAgri + "\",\n" +
                    "nous vous informons que la planification est fixee pour le " + date + ".\n\n" +
                    "Note du technicien :\n" +
                    descriptionTechnicien + "\n\n" +
                    "Details financiers :\n" +
                    "- Cout estime : " + cout + " DT\n\n" +
                    "Veuillez trouver ci-joint le rapport officiel de cette intervention.\n\n" +
                    "L'equipe AgriCore vous remercie pour votre confiance !";
            messageBodyPart.setText(corpsEmail);

            // 2. Création de la pièce jointe (Le PDF)
            MimeBodyPart attachmentPart = new MimeBodyPart();
            // Utilise la variable 'filename' que tu as créée pour le PDF
            attachmentPart.attachFile(new java.io.File(filename));

            // 3. On rassemble le texte et le PDF dans un conteneur Multipart
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart); // Ajoute le texte
            multipart.addBodyPart(attachmentPart);  // Ajoute le fichier

            // 4. On met ce contenu dans le message
            message.setContent(multipart);

            // Envoi
            Transport.send(message);
            System.out.println("Email envoyé avec succès avec le PDF !");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}