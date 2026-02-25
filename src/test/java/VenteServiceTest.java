import models.User;
import models.Vente;
import org.junit.jupiter.api.*;
import services.UserService;
import services.VenteService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VenteServiceTest {
    static VenteService vs;
    static UserService us;
    private int idVente = -1;
    private int idUser = -1;

    @BeforeAll
    public static void setup() {
        vs = new VenteService();
        us = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idVente != -1) {
            vs.delete(idVente);
            idVente = -1;
        }
        if (idUser != -1) {
            us.delete(idUser);
            idUser = -1;
        }
    }

    @Test
    @Order(1)
    public void testCreateVente() throws SQLException {
        idUser = us.create(new User(22, "test_vente", "user"));
        Vente v = new Vente();
        v.setUserId(idUser);
        v.setPrixUnitaire(10.0);
        v.setQuantite(5.0);
        v.setChiffreAffaires(50.0);
        v.setDate(LocalDate.now());
        v.setProduit("ProduitTest");
        vs.create(v);

        List<Vente> ventes = vs.readByUser(idUser);
        assertFalse(ventes.isEmpty());
        Vente created = ventes.stream()
                .filter(x -> "ProduitTest".equals(x.getProduit()) && x.getChiffreAffaires() == 50.0)
                .findFirst()
                .orElse(null);
        assertTrue(created != null);
        if (created != null) {
            idVente = created.getIdVente();
        }
    }

    @Test
    @Order(2)
    public void testUpdateVente() throws SQLException {
        idUser = us.create(new User(22, "test_vente_upd", "user"));
        Vente v = new Vente();
        v.setUserId(idUser);
        v.setPrixUnitaire(20.0);
        v.setQuantite(2.0);
        v.setChiffreAffaires(40.0);
        v.setDate(LocalDate.now());
        v.setProduit("Avant");
        vs.create(v);

        Vente created = vs.readByUser(idUser).stream()
                .filter(x -> "Avant".equals(x.getProduit()) && x.getChiffreAffaires() == 40.0)
                .findFirst()
                .orElseThrow();
        idVente = created.getIdVente();

        created.setProduit("Apres");
        created.setQuantite(3.0);
        created.setChiffreAffaires(60.0);
        vs.update(created);

        boolean found = vs.readByUser(idUser).stream()
                .anyMatch(x -> x.getIdVente() == idVente && "Apres".equals(x.getProduit()) && x.getChiffreAffaires() == 60.0);
        assertTrue(found);
    }
}
