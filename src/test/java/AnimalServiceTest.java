import models.Animal;
import org.junit.jupiter.api.*;
import services.AnimalService;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnimalServiceTest {

    static AnimalService animalService;
    private int lastInsertedId = -1;

    @BeforeAll
    public static void setup() {
        animalService = new AnimalService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (lastInsertedId != -1) {
            animalService.delete(lastInsertedId);
            lastInsertedId = -1;
        }
    }

    @Test
    @Order(1)
    void testCreateAnimal() throws SQLException {
        Animal a = new Animal(1, "AN001", "Chien", "Berger", "M", Date.valueOf(LocalDate.now()));
        int id = animalService.create(a);
        lastInsertedId = id;

        assertTrue(id > 0, "L'ID doit être supérieur à 0");

        Animal created = animalService.readById(id);
        assertNotNull(created);
        assertEquals("AN001", created.getCodeAnimal());
        assertEquals("Chien", created.getEspece());
    }

    @Test
    @Order(2)
    void testUpdateAnimal() throws SQLException {
        Animal a = new Animal(1, "AN002", "Chat", "Siamois", "F", Date.valueOf(LocalDate.now()));
        int id = animalService.create(a);
        lastInsertedId = id;

        // Modifier l'espèce
        a.setIdAnimal(id);
        a.setEspece("Chat Modifié");
        animalService.update(a);

        Animal updated = animalService.readById(id);
        assertEquals("Chat Modifié", updated.getEspece());
    }

    @Test
    @Order(3)
    void testDeleteAnimal() throws SQLException {
        Animal a = new Animal(1, "AN003", "Lapin", "Nain", "M", Date.valueOf(LocalDate.now()));
        int id = animalService.create(a);

        animalService.delete(id);
        Animal deleted = animalService.readById(id);
        assertNull(deleted, "L'animal doit être supprimé");
    }

    @Test
    @Order(4)
    void testReadAnimals() throws SQLException {
        List<Animal> list = animalService.read();
        assertNotNull(list);
        assertTrue(list.size() >= 0);
    }
}