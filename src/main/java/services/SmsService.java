package services; // Vérifie que le nom du package correspond au tien

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsService {


    public static final String ACCOUNT_SID = "VOTRE_SID_ICI";
    public static final String AUTH_TOKEN = "VOTRE_TOKEN_ICI";
    public static final String TWILIO_NUMBER = "+18382406798";

    public static void envoyerSms(String toPhone, String content) {
        try {

            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

            // Envoie le message
            Message message = Message.creator(
                    new PhoneNumber(toPhone),      // Numéro du destinataire
                    new PhoneNumber(TWILIO_NUMBER), // Ton numéro expéditeur Twilio
                    content                        // Le texte du SMS
            ).create();

            System.out.println(" SMS envoyé avec succès ! SID: " + message.getSid());

        } catch (Exception e) {
            // Affiche l'erreur si le SMS ne part pas
            System.err.println("Erreur lors de l'envoi du SMS : " + e.getMessage());
        }
    }
}