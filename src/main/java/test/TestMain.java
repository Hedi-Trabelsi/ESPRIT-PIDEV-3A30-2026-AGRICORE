
package test;
import models.EvennementAgricole;
import models.Participant;
import services.EvennementService;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class TestMain {
    public static void main(String[] args) {
        EvennementService evenService = new EvennementService();
        ParticipantService partService = new ParticipantService();

        try {
            // --- Test Evennement ---
            EvennementAgricole e = new EvennementAgricole(
                    "Salon Agricole",
                    "Exposition des innovations agricoles",
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 5),
                    "Tunis",
                    100,
                    50,
                    "Ouvert"
            );
            int idE = evenService.create(e);
            System.out.println("Evennement created with ID: " + idE);

            List<EvennementAgricole> evenList = evenService.read();
            evenList.forEach(System.out::println);

            e.setIdEvennement(idE);
            e.setTitre("Salon Agricole 2026");
            evenService.update(e);
            System.out.println("Evennement updated");

            // --- Test Participant ---
            Participant p = new Participant(
                    1,      // idUtilisateur
                    idE,    // idEvennement
                    LocalDate.now(),
                    "Inscrit",
                    "50",
                    "Non"
            );
            int idP = partService.create(p);
            System.out.println("Participant created with ID: " + idP);

            List<Participant> partList = partService.read();
            partList.forEach(System.out::println);

            p.setIdParticipant(idP);
            p.setConfirmation("Oui");
            partService.update(p);
            System.out.println("Participant updated");

            // Delete (optional test)
            // partService.delete(idP);
            // evenService.delete(idE);
            // System.out.println("Deleted participant and event");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
