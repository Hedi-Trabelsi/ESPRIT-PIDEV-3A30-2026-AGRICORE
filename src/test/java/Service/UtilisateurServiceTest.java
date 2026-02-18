package Service;

import Model.Utilisateur;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UtilisateurServiceTest {

    static UtilisateurService service;

    @BeforeAll
    static void setup() {
        service = new UtilisateurService();
    }

    // ✅ Test 1: Database Connection
    @Test
    @Order(1)
    void testDatabaseConnection() {
        assertTrue(service.isConnected(), "Database should be connected");
    }

    // ✅ Test 2: Add User
    @Test
    @Order(2)
    void testAjouterUtilisateur() throws SQLException {

        Utilisateur u = new Utilisateur(
                "TestNom",
                "TestPrenom",
                LocalDate.of(2000, 1, 1),
                "Male",
                "Test Adresse",
                12345678,   // int phone
                1           // role = 1 (ex: Technicien)
        );

        service.ajouter(u);

        List<Utilisateur> list = service.afficher();

        boolean existe = list.stream()
                .anyMatch(user -> user.getNom().equals("TestNom"));

        assertTrue(existe, "User should exist after insertion");
    }

    // ✅ Test 3: Delete User
    @Test
    @Order(3)
    void testSupprimerUtilisateur() throws SQLException {

        List<Utilisateur> list = service.afficher();

        assertFalse(list.isEmpty(), "User list should not be empty");

        Utilisateur last = list.get(list.size() - 1);

        service.supprimer(last.getId());

        List<Utilisateur> newList = service.afficher();

        boolean existe = newList.stream()
                .anyMatch(user -> user.getId() == last.getId());

        assertFalse(existe, "User should be deleted");
    }
}
