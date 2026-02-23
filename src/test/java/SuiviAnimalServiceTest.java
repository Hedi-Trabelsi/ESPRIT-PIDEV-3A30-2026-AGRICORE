import models.Animal;
import models.SuiviAnimal;
import org.junit.jupiter.api.*;
import services.AnimalService;
import services.SuiviAnimalService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SuiviAnimalServiceTest {

    private static SuiviAnimalService suiviService;
    private static AnimalService animalService;
    private static int idAnimalExistant; // ✅ ID réel depuis la BDD
    private static int idSuiviCree = -1;

    @BeforeAll
    static void setup() throws SQLException {
        suiviService  = new SuiviAnimalService();
        animalService = new AnimalService();

        // ✅ Récupérer un vrai idAnimal existant dans la BDD
        List<Animal> animaux = animalService.read();
        assertFalse(animaux.isEmpty(),
                "❌ La table 'animal' est vide ! Ajoutez au moins un animal avant de tester.");
        idAnimalExistant = animaux.get(0).getIdAnimal();
        System.out.println("✅ idAnimal utilisé pour les tests : " + idAnimalExistant);
    }

    // ════════════════════════════════════════════════
    //  TEST 1 : CREATE
    // ════════════════════════════════════════════════
    @Test
    @Order(1)
    void testCreateSuivi() throws SQLException {
        SuiviAnimal s = new SuiviAnimal(
                idAnimalExistant,                          // ✅ ID réel
                Timestamp.valueOf(LocalDateTime.now()),
                38.5,
                450.0,
                70,
                "Bon",
                "Test remarque",
                "Moyen"
        );

        int id = suiviService.create(s);
        idSuiviCree = id;

        assertTrue(id > 0, "❌ L'ID retourné doit être > 0");
        System.out.println("✅ Suivi créé avec idSuivi = " + id);
    }

    // ════════════════════════════════════════════════
    //  TEST 2 : READ
    // ════════════════════════════════════════════════
    @Test
    @Order(2)
    void testReadSuivi() throws SQLException {
        List<SuiviAnimal> list = suiviService.read();
        assertNotNull(list, "❌ La liste ne doit pas être null");
        assertFalse(list.isEmpty(), "❌ La liste ne doit pas être vide");
        System.out.println("✅ Nombre de suivis lus : " + list.size());
    }

    // ════════════════════════════════════════════════
    //  TEST 3 : UPDATE
    // ════════════════════════════════════════════════
    @Test
    @Order(3)
    void testUpdateSuivi() throws SQLException {
        // Créer un suivi pour le modifier
        SuiviAnimal s = new SuiviAnimal(
                idAnimalExistant,
                Timestamp.valueOf(LocalDateTime.now()),
                39.0,
                460.0,
                75,
                "Malade",
                "Remarque update",
                "Faible"
        );
        int id = suiviService.create(s);
        s.setIdSuivi(id);

        // Modifier
        s.setTemperature(40.0);
        s.setEtatSante("Critique");
        s.setRemarque("Modifié par test");
        suiviService.update(s);

        // Vérifier
        List<SuiviAnimal> list = suiviService.read();
        SuiviAnimal updated = list.stream()
                .filter(x -> x.getIdSuivi() == id)
                .findFirst()
                .orElse(null);

        assertNotNull(updated, "❌ Suivi non trouvé après update");
        assertEquals(40.0, updated.getTemperature(), "❌ Température non mise à jour");
        assertEquals("Critique", updated.getEtatSante(), "❌ État non mis à jour");
        System.out.println("✅ Suivi mis à jour avec succès");

        // Nettoyage
        suiviService.delete(id);
    }

    // ════════════════════════════════════════════════
    //  TEST 4 : DELETE
    // ════════════════════════════════════════════════
    @Test
    @Order(4)
    void testDeleteSuivi() throws SQLException {
        // Créer un suivi pour le supprimer
        SuiviAnimal s = new SuiviAnimal(
                idAnimalExistant,
                Timestamp.valueOf(LocalDateTime.now()),
                37.5,
                400.0,
                65,
                "Bon",
                "À supprimer",
                "Élevé"
        );
        int id = suiviService.create(s);
        assertTrue(id > 0, "❌ Création échouée avant suppression");

        // Supprimer
        suiviService.delete(id);

        // Vérifier que c'est supprimé
        List<SuiviAnimal> list = suiviService.read();
        boolean existe = list.stream().anyMatch(x -> x.getIdSuivi() == id);
        assertFalse(existe, "❌ Le suivi existe encore après suppression !");
        System.out.println("✅ Suivi supprimé avec succès");
    }

    // ════════════════════════════════════════════════
    //  NETTOYAGE FINAL
    // ════════════════════════════════════════════════
    @AfterAll
    static void cleanup() throws SQLException {
        // Supprimer le suivi créé dans testCreate si pas encore supprimé
        if (idSuiviCree > 0) {
            try { suiviService.delete(idSuiviCree); }
            catch (Exception ignored) {}
        }
        System.out.println("✅ Nettoyage terminé");
    }
}