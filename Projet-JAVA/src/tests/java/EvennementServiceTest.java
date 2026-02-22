
import models.EvennementAgricole;
import org.junit.jupiter.api.*;
import services.EvennementService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EvennementServiceTest {

    static EvennementService es;
    private int idEvennement = -1;

    @BeforeAll
    public static void setup() {
        es = new EvennementService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idEvennement != -1) {
            es.delete(idEvennement);
            System.out.println("[DEBUG_LOG] Cleanup: Deleted Evennement with ID: " + idEvennement);
            idEvennement = -1;
        }
    }

    @Test
    @Order(1)
    public void testCreateEvennement() {
        EvennementAgricole e = new EvennementAgricole(
                "Foire Agricole",
                "Description test",
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                "Tunis",
                100,
                50,
                "Actif"
        );

        try {
            int id = es.create(e);
            this.idEvennement = id;
            System.out.println("[DEBUG_LOG] Created Evennement with ID: " + id);
            assertTrue(id > 0, "Evennement ID should be greater than 0");

            List<EvennementAgricole> events = es.read();
            assertFalse(events.isEmpty());

            boolean found = events.stream().anyMatch(ev -> ev.getIdEvennement() == id && ev.getTitre().equals("Foire Agricole"));
            if (found) System.out.println("[DEBUG_LOG] Verified: Evennement exists in DB.");

            assertTrue(found, "Evennement should exist in DB after creation");

        } catch (SQLException ex) {
            System.out.println("[DEBUG_LOG] Exception in testCreateEvennement: " + ex.getMessage());
        }
    }

    @Test
    @Order(2)
    public void testUpdateEvennement() {
        EvennementAgricole e = new EvennementAgricole(
                "Foire Agricole",
                "Description test",
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                "Tunis",
                100,
                50,
                "Actif"
        );

        try {
            int id = es.create(e);
            this.idEvennement = id;
            System.out.println("[DEBUG_LOG] Created Evennement with ID: " + id);

            EvennementAgricole updateInfo = new EvennementAgricole();
            updateInfo.setIdEvennement(id);
            updateInfo.setTitre("Foire Update");
            updateInfo.setDescription("Description modifiée");
            updateInfo.setDateDebut(LocalDate.now());
            updateInfo.setDateFin(LocalDate.now().plusDays(3));
            updateInfo.setLieu("Sousse");
            updateInfo.setCapaciteMax(150);
            updateInfo.setFraisInscription(75);
            updateInfo.setStatut("Inactif");

            es.update(updateInfo);
            System.out.println("[DEBUG_LOG] Updated Evennement ID " + id + " to title 'Foire Update'");

            List<EvennementAgricole> events = es.read();
            assertFalse(events.isEmpty());

            boolean found = events.stream().anyMatch(ev -> ev.getIdEvennement() == id && ev.getTitre().equals("Foire Update"));
            if (found) System.out.println("[DEBUG_LOG] Verified: Evennement update reflected in DB.");

            assertTrue(found, "Evennement should reflect updated title in DB");

        } catch (SQLException ex) {
            System.out.println("[DEBUG_LOG] Exception in testUpdateEvennement: " + ex.getMessage());
        }
    }
}
