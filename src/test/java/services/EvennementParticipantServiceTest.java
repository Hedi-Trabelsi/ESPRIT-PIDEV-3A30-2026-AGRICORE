package services;

import models.EvennementAgricole;
import models.Participant;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EvennementParticipantServiceTest {

    private static EvennementService evenService;
    private static ParticipantService partService;
    private static int lastEventId;
    private static int lastParticipantId;

    @BeforeAll
    static void setup() {
        evenService = new EvennementService();
        partService = new ParticipantService();
    }

    @Test
    @Order(1)
    void testCreateEvennement() throws SQLException {
        EvennementAgricole e = new EvennementAgricole();
        e.setTitre("Test Event");
        e.setDescription("Desc");
        e.setDateDebut(LocalDate.now());
        e.setDateFin(LocalDate.now().plusDays(2));
        e.setLieu("LieuTest");
        e.setCapaciteMax(100);
        e.setFraisInscription(50);
        e.setStatut("Actif");

        lastEventId = evenService.create(e);
        Assertions.assertTrue(lastEventId > 0);
    }

    @Test
    @Order(2)
    void testCreateParticipant() throws SQLException {
        Participant p = new Participant();
        p.setIdUtilisateur(1); // make sure user id 1 exists
        p.setIdEvennement(lastEventId);
        p.setDateInscription(LocalDate.now());
        p.setStatutParticipation("Inscrit");
        p.setMontantPayee("50");
        p.setConfirmation("Oui");

        lastParticipantId = partService.create(p);
        Assertions.assertTrue(lastParticipantId > 0);
    }

    @Test
    @Order(3)
    void testReadEvennements() throws SQLException {
        List<EvennementAgricole> events = evenService.read();
        Assertions.assertFalse(events.isEmpty());
    }

    @Test
    @Order(4)
    void testReadParticipants() throws SQLException {
        List<Participant> parts = partService.read();
        Assertions.assertFalse(parts.isEmpty());
    }

    @Test
    @Order(5)
    void testDeleteParticipantAndEvent() throws SQLException {
        partService.delete(lastParticipantId);
        evenService.delete(lastEventId);
    }
}
