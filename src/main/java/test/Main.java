package test;

import models.Animal;
import services.AnimalService;

import java.sql.Date;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {

        AnimalService ps = new AnimalService();

        try {

            Animal a = new Animal(
                    1,                      // idAgriculteur
                    "A001",                 // codeAnimal
                    "Bovin",                // espece
                    "Holstein",             // race
                    "Femelle",              // sexe
                    Date.valueOf("2023-01-10")  // dateNaissance
            );

            ps.create(a);

            System.out.println(ps.read());

        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}