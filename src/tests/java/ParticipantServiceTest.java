import models.Participant;
import org.junit.jupiter.api.*;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ParticipantServiceTest {

    static ParticipantService ps;
    private int idParticipant = -1;

    @BeforeAll
    public static void setup() {
        ps = new ParticipantService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idParticipant != -1) {
            ps.delete(idParticipant);
            System.out.println("[DEBUG_LOG] Cleanup: Deleted Participant with ID: " + idParticipant);
            idParticipant = -1;
        }
    }

    @Test
    @Order(1)
    public void testCreateParticipant() {
        Participant p = new Participant(
                1, // idUtilisateur
                1, // idEvennement
                LocalDate.now(),
                "Inscrit",
                "50",
                "Oui"
        );

        try {
            int id = ps.create(p);
            this.idParticipant = id;
            System.out.println("[DEBUG_LOG] Created Participant with ID: " + id);

            assertTrue(id > 0, "Participant ID should be greater than 0");

            List<Participant> participants = ps.read();
            assertFalse(participants.isEmpty());

            boolean found = participants.stream().anyMatch(part -> part.getIdParticipant() == id && part.getStatutParticipation().equals("Inscrit"));
            if (found) System.out.println("[DEBUG_LOG] Verified: Participant exists in DB.");

            assertTrue(found, "Participant should exist in DB after creation");

        } catch (SQLException ex) {
            System.out.println("[DEBUG_LOG] Exception in testCreateParticipant: " + ex.getMessage());
        }
    }

    @Test
    @Order(2)
    public void testUpdateParticipant() {
        Participant p = new Participant(
                1, 1, LocalDate.now(), "Inscrit", "50", "Oui"
        );

        try {
            int id = ps.create(p);
            this.idParticipant = id;
            System.out.println("[DEBUG_LOG] Created Participant with ID: " + id);

            Participant updateInfo = new Participant();
            updateInfo.setIdParticipant(id);
            updateInfo.setStatutParticipation("Présent");
            updateInfo.setMontantPayee("75");
            updateInfo.setConfirmation("Oui");

            ps.update(updateInfo);
            System.out.println("[DEBUG_LOG] Updated Participant ID " + id + " to status 'Présent'");

            List<Participant> participants = ps.read();
            assertFalse(participants.isEmpty());

            boolean found = participants.stream().anyMatch(part -> part.getIdParticipant() == id && part.getStatutParticipation().equals("Présent"));
            if (found) System.out.println("[DEBUG_LOG] Verified: Participant update reflected in DB.");

            assertTrue(found, "Participant should reflect updated status in DB");

        } catch (SQLException ex) {
            System.out.println("[DEBUG_LOG] Exception in testUpdateParticipant: " + ex.getMessage());
        }
    }
}
