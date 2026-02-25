import models.Depense;
import models.TypeDepense;
import models.User;
import org.junit.jupiter.api.*;
import services.DepenseService;
import services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DepenseServiceTest {
    static DepenseService ds;
    static UserService us;
    private int idDepense = -1;
    private int idUser = -1;

    @BeforeAll
    public static void setup() {
        ds = new DepenseService();
        us = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idDepense != -1) {
            ds.delete(idDepense);
            idDepense = -1;
        }
        if (idUser != -1) {
            us.delete(idUser);
            idUser = -1;
        }
    }

    @Test
    @Order(1)
    public void testCreateDepense() throws SQLException {
        idUser = us.create(new User(22, "test_dep", "user"));
        Depense d = new Depense();
        d.setUserId(idUser);
        d.setType(TypeDepense.AUTRE);
        d.setMontant(123.45);
        d.setDate(LocalDate.now());
        ds.create(d);

        List<Depense> depenses = ds.readByUser(idUser);
        assertFalse(depenses.isEmpty());
        Depense created = depenses.stream()
                .filter(x -> x.getMontant() == 123.45 && TypeDepense.AUTRE.equals(x.getType()))
                .findFirst()
                .orElse(null);
        assertTrue(created != null);
        if (created != null) {
            idDepense = created.getIdDepense();
        }
    }

    @Test
    @Order(2)
    public void testUpdateDepense() throws SQLException {
        idUser = us.create(new User(22, "test_dep_upd", "user"));
        Depense d = new Depense();
        d.setUserId(idUser);
        d.setType(TypeDepense.CARBURANT);
        d.setMontant(50.0);
        d.setDate(LocalDate.now());
        ds.create(d);
        List<Depense> depenses = ds.readByUser(idUser);
        Depense created = depenses.stream()
                .filter(x -> x.getMontant() == 50.0 && TypeDepense.CARBURANT.equals(x.getType()))
                .findFirst()
                .orElseThrow();
        idDepense = created.getIdDepense();

        created.setMontant(75.0);
        created.setType(TypeDepense.INTRANT);
        ds.update(created);

        List<Depense> after = ds.readByUser(idUser);
        boolean found = after.stream()
                .anyMatch(x -> x.getIdDepense() == idDepense && x.getMontant() == 75.0 && TypeDepense.INTRANT.equals(x.getType()));
        assertTrue(found);
    }
}
