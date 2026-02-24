import entities.Equipement;
import entities.Panier;
import org.junit.jupiter.api.*;
import services.EquipementService;
import services.PanierService;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PanierServiceTest {

    static PanierService ps;
    static EquipementService es;
    private int idPanier = -1;
    private int idEquipement = -1;

    @BeforeAll
    static void setup() {
        ps = new PanierService();
        es = new EquipementService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idPanier != -1) {
            ps.supprimer(idPanier);
            System.out.println("[DEBUG] Cleanup: Deleted Panier with ID " + idPanier);
            idPanier = -1;
        }
        if (idEquipement != -1) {
            es.supprimer(idEquipement);
            System.out.println("[DEBUG] Cleanup: Deleted Equipement with ID " + idEquipement);
            idEquipement = -1;
        }
    }

    @Test
    @Order(1)
    void testAjouter() throws SQLException {
        // Créer un équipement nécessaire pour le panier
        Equipement e = new Equipement("TracteurTest", "Machine", "1000", 1, 1);
        es.ajouter(e);
        List<Equipement> equipList = es.afficher();
        idEquipement = equipList.stream().filter(eq -> eq.getNom().equals("TracteurTest")).findFirst().get().getId_equipement();

        // Ajouter le panier
        Panier p = new Panier(idEquipement, 3, "15000", 1);
        ps.ajouter(p);

        List<Panier> list = ps.afficher();
        assertFalse(list.isEmpty());
        boolean found = list.stream().anyMatch(pa -> pa.getQuantite() == 3 && pa.getId_equipement() == idEquipement);
        assertTrue(found, "Le panier doit contenir l'article ajouté");

        // Sauvegarder ID pour cleanup
        idPanier = list.stream().filter(pa -> pa.getQuantite() == 3).findFirst().get().getId_panier();
    }

    @Test
    @Order(2)
    void testSupprimer() throws SQLException {
        // Créer un équipement pour le panier
        Equipement e = new Equipement("PulvérisateurTest", "Machine", "2000", 1, 1);
        es.ajouter(e);
        List<Equipement> equipList = es.afficher();
        idEquipement = equipList.stream().filter(eq -> eq.getNom().equals("PulvérisateurTest")).findFirst().get().getId_equipement();

        // Ajouter le panier
        Panier p = new Panier(idEquipement, 1, "5000", 1);
        ps.ajouter(p);

        List<Panier> list = ps.afficher();
        idPanier = list.stream().filter(pa -> pa.getId_equipement() == idEquipement).findFirst().get().getId_panier();

        // Supprimer
        ps.supprimer(idPanier);

        list = ps.afficher();
        boolean exists = list.stream().anyMatch(pa -> pa.getId_panier() == idPanier);
        assertFalse(exists, "Le panier doit être supprimé");

        idPanier = -1;
    }
}
