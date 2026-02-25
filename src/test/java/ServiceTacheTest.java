
import Model.Tache;
import org.junit.jupiter.api.*;
import services.ServiceTache;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceTacheTest {

    static ServiceTache service;
    static int idTacheTest; // id créé pour les tests

    @BeforeAll
    static void setup() {
        service = new ServiceTache();
    }

    @Test
    @Order(1)
    void testAjouterTache() throws SQLException {
        // Créer la tâche
        Tache t = new Tache();
        t.setDate_prevue(LocalDate.now().toString());
        t.setDesciption("Description test"); // correspond à description dans la DB
        t.setCout_estimee(100);
        t.setId_maintenace(35); // mettre un id_maintenance valide

        service.ajouter(t);

        List<Tache> taches = service.afficher();
        assertFalse(taches.isEmpty());

        // Récupérer l'id créé pour les tests suivants
        Tache last = taches.get(taches.size() - 1);
        idTacheTest = last.getId_tache();

        assertTrue(taches.stream()
                .anyMatch(tt -> "Description test".equals(tt.getDesciption())));
    }

    @Test
    @Order(2)
    void testModifierTache() throws SQLException {

        Tache tache = new Tache();
        tache.setId_tache(idTacheTest);
        tache.setDesciption("Description modifiée");
        tache.setCout_estimee(150);


        service.modifier(tache);


        List<Tache> taches = service.afficher();


        boolean trouve = taches.stream()
                .anyMatch(tt -> tt.getId_tache() == idTacheTest
                        && "Description modifiée".equals(tt.getDesciption())
                        && tt.getCout_estimee() == 150);

        assertTrue(trouve, "La tâche n'a pas été correctement modifiée");
    }

    @Test
    @Order(3)
    void testSupprimerTache() throws SQLException {
        service.supprimer(idTacheTest);

        List<Tache> taches = service.afficher();
        assertFalse(taches.stream().anyMatch(tt -> tt.getId_tache() == idTacheTest));
    }
}
