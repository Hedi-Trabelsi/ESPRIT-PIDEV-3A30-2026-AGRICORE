package tests;
import services.ServiceMaintenance;
import entities.Maintenance;

import java.time.LocalDate;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        ServiceMaintenance sm = new ServiceMaintenance();

        try {
            // Ajouter une maintenance
         /*   Maintenance m1 = new Maintenance(
                    "Révision",                 // type
                    LocalDate.now(),            // dateDeclaration
                    "Révision annuelle de la machine", // description
                    "Oui",                      // statutory
                    1                           // idTechnicien
            );

            sm.ajouter(m1);
            System.out.println("Maintenance ajoutée !");*/
            // 2 Modifier la maintenance

         /*   Maintenance mToModify = new Maintenance(
                    2,                              // id de la maintenance à modifier
                    "Révision",                     // type
                    LocalDate.now(),                // dateDeclaration
                    "Description mise à jour",      // nouvelle description
                    "Oui",                          // statut
                    1                               // idTechnicien
            );
            sm.modifier(mToModify);
            System.out.println("Maintenance modifiée !");*/
            sm.supprimer(3);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
