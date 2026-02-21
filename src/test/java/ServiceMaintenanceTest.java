
import models.Maintenance;
import org.junit.jupiter.api.*;
import services.ServiceMaintenance;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceMaintenanceTest {

    static ServiceMaintenance service;
    static int idMaintenanceTest; // id créé pour les tests

    @BeforeAll
    static void setup() {
        service = new ServiceMaintenance();
    }

    @Test
    @Order(1)
    void testAjouterMaintenance() throws SQLException {
        Maintenance m = new Maintenance();
        m.setType("Électrique");
        m.setDateDeclaration(LocalDate.now());
        m.setDescription("Test ajout");
        m.setStatut("planifie");
        m.setIdTechnicien(1);
        m.setPriorite("normale");
        m.setLieu("Site Test");
        m.setEquipement("Equipement Test");

        service.ajouter(m);

        List<Maintenance> maints = service.afficher();
        assertFalse(maints.isEmpty());

        // Récupérer l'id créé pour les tests suivants
        Maintenance last = maints.get(maints.size() - 1);
        idMaintenanceTest = last.getId();

        assertTrue(maints.stream()
                .anyMatch(mt -> mt.getDescription().equals("Test ajout")));
    }

    @Test
    @Order(2)
    void testModifierMaintenance() throws SQLException {
        // Récupérer l'objet complet depuis la DB pour éviter null
        List<Maintenance> maints = service.afficher();
        Maintenance m = maints.stream()
                .filter(mt -> mt.getId() == idMaintenanceTest)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Maintenance test introuvable"));

        // Modifier les champs
        m.setDescription("Maintenance modifiée");
        m.setStatut("en cours");
        m.setPriorite("urgente");

        service.modifier(m);

        // Vérifier la modification
        List<Maintenance> updated = service.afficher();
        assertTrue(updated.stream()
                .anyMatch(mt -> mt.getId() == idMaintenanceTest
                        && "Maintenance modifiée".equals(mt.getDescription())
                        && "en cours".equals(mt.getStatut())
                        && "urgente".equals(mt.getPriorite())));
    }

    @Test
    @Order(3)
    void testSupprimerMaintenance() throws SQLException {
        service.supprimer(idMaintenanceTest);

        List<Maintenance> maints = service.afficher();
        assertFalse(maints.stream().anyMatch(mt -> mt.getId() == idMaintenanceTest));
    }
}
