package test;

import models.EvennementAgricole;
import models.Participant;
import services.EvennementService;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        EvennementService es = new EvennementService();
        ParticipantService ps = new ParticipantService();

        try {
            // Ajouter un événement
            EvennementAgricole e1 = new EvennementAgricole(
                    "Salon Agricole",
                    "Salon des machines",
                    LocalDate.parse("2026-03-01"),  // <-- LocalDate
                    LocalDate.parse("2026-03-03"),  // <-- LocalDate
                    "Tunis",
                    100,
                    50,
                    "Actif"
            );
            int evennementId = es.create(e1);
            e1.setIdEvennement(evennementId); // important to set ID after creation

            // Ajouter un participant
            Participant p1 = new Participant(
                    1,                        // id_utilisateur
                    e1.getIdEvennement(),     // id_evennement
                    LocalDate.parse("2026-02-15"), // date_inscription
                    "Inscrit",
                    "50",
                    "Oui"
            );
            int participantId = ps.create(p1);
            p1.setIdParticipant(participantId);

            // Lire tous les événements
            List<EvennementAgricole> evennements = es.read();
            System.out.println("Événements : " + evennements);

            // Lire tous les participants
            List<Participant> participants = ps.read();
            System.out.println("Participants : " + participants);

        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
