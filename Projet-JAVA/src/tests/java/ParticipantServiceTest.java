import models.Participant;
import org.junit.jupiter.api.*;
import services.ParticipantService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
            idParticipant = -1;
        }
    }

    @Test
    @Order(1)
    public void testCreateParticipant() {

        Participant p = new Participant(
                1,                // idUtilisateur
                1,                // idEvennement
                LocalDate.now(),
                "Inscrit",
                "50",
                "Oui",
                2,                // nbrPlaces
                "Test User"       // nomParticipant
        );

        try {
            int id = ps.create(p);
            this.idParticipant = id;

            assertTrue(id > 0);

            List<Participant> participants = ps.read();
            assertFalse(participants.isEmpty());

            boolean found = participants.stream()
                    .anyMatch(part ->
                            part.getIdParticipant() == id &&
                                    part.getStatutParticipation().equals("Inscrit")
                    );

            assertTrue(found);

        } catch (SQLException e) {
            fail("Exception during create test: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    public void testUpdateParticipant() {

        Participant p = new Participant(
                1,
                1,
                LocalDate.now(),
                "Inscrit",
                "50",
                "Oui",
                1,
                "Update Test"
        );

        try {
            int id = ps.create(p);
            this.idParticipant = id;

            Participant updateInfo = new Participant();
            updateInfo.setIdParticipant(id);
            updateInfo.setStatutParticipation("Présent");
            updateInfo.setMontantPayee("75");
            updateInfo.setConfirmation("Oui");

            ps.update(updateInfo);

            List<Participant> participants = ps.read();

            boolean found = participants.stream()
                    .anyMatch(part ->
                            part.getIdParticipant() == id &&
                                    part.getStatutParticipation().equals("Présent") &&
                                    part.getMontantPayee().equals("75")
                    );

            assertTrue(found);

        } catch (SQLException e) {
            fail("Exception during update test: " + e.getMessage());
        }
    }
}
