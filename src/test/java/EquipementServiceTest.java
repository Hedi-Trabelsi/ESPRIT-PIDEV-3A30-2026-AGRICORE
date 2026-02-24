import entities.Equipement;
import org.junit.jupiter.api.*;
import services.EquipementService;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EquipementServiceTest {

    static EquipementService es;
    private int idEquipement = -1;

    @BeforeAll
    static void setup() {
        es = new EquipementService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idEquipement != -1) {
            es.supprimer(idEquipement);
            System.out.println("[DEBUG] Cleanup: Deleted Equipement with ID " + idEquipement);
            idEquipement = -1;
        }
    }

    @Test
    @Order(1)
    void testAjouter() throws SQLException {
        Equipement e = new Equipement("Tracteur","Machine","50000",2,1);
        es.ajouter(e);

        List<Equipement> list = es.afficher();
        assertFalse(list.isEmpty());
        boolean found = list.stream().anyMatch(eq -> eq.getNom().equals("Tracteur"));
        assertTrue(found, "L'équipement doit exister après ajout");

        // sauvegarder ID pour cleanup
        idEquipement = list.stream()
                .filter(eq -> eq.getNom().equals("Tracteur"))
                .findFirst()
                .get()
                .getId_equipement();
    }

    @Test
    @Order(2)
    void testModifier() throws SQLException {
        // Ajouter un équipement
        Equipement e = new Equipement("Charrue","Outil","1500",1,1);
        es.ajouter(e);
        List<Equipement> list = es.afficher();
        idEquipement = list.stream().filter(eq -> eq.getNom().equals("Charrue")).findFirst().get().getId_equipement();

        // Modifier
        Equipement update = new Equipement(idEquipement,"CharrueMod","Outil","2000",3,1);
        es.modifier(update);

        // Vérification
        list = es.afficher();
        boolean found = list.stream().anyMatch(eq -> eq.getId_equipement() == idEquipement && eq.getNom().equals("CharrueMod"));
        assertTrue(found, "L'équipement doit être modifié");
    }

    @Test
    @Order(3)
    void testSupprimer() throws SQLException {
        Equipement e = new Equipement("Pulvérisateur","Machine","2500",1,1);
        es.ajouter(e);
        List<Equipement> list = es.afficher();
        idEquipement = list.stream().filter(eq -> eq.getNom().equals("Pulvérisateur")).findFirst().get().getId_equipement();

        es.supprimer(idEquipement);

        list = es.afficher();
        boolean exists = list.stream().anyMatch(eq -> eq.getId_equipement() == idEquipement);
        assertFalse(exists, "L'équipement doit être supprimé");
        idEquipement = -1;
    }
}
