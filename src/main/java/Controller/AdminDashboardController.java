package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import Model.Animal;
import Model.SuiviAnimal;
import services.AnimalService;
import services.SuiviAnimalService;

import java.sql.SQLException;
import java.util.List;

public class AdminDashboardController {

    @FXML private Label lblTotalAnimaux;
    @FXML private Label lblTotalSuivis;
    @FXML private Label lblAnimauxMalades;
    @FXML private Label lblAnimauxCritiques;
    @FXML private VBox  activiteBox;

    private final AnimalService      animalService = new AnimalService();
    private final SuiviAnimalService suiviService  = new SuiviAnimalService();

    @FXML
    void initialize() {
        chargerStatistiques();
    }

    private void chargerStatistiques() {
        try {
            List<Animal>      animaux = animalService.read();
            List<SuiviAnimal> suivis  = suiviService.read();

            lblTotalAnimaux.setText(String.valueOf(animaux.size()));
            lblTotalSuivis.setText(String.valueOf(suivis.size()));

            long malades   = suivis.stream().filter(s -> "Malade".equals(s.getEtatSante())).count();
            long critiques = suivis.stream().filter(s -> "Critique".equals(s.getEtatSante())).count();
            lblAnimauxMalades.setText(String.valueOf(malades));
            lblAnimauxCritiques.setText(String.valueOf(critiques));

            activiteBox.getChildren().clear();
            suivis.stream()
                    .sorted((a, b) -> b.getDateSuivi().compareTo(a.getDateSuivi()))
                    .limit(8)
                    .forEach(s -> {
                        String nomAnimal = animaux.stream()
                                .filter(a -> a.getIdAnimal() == s.getIdAnimal())
                                .map(a -> a.getCodeAnimal() + " (" + a.getEspece() + ")")
                                .findFirst().orElse("Inconnu");

                        HBox row = new HBox(15);
                        row.setStyle("-fx-padding:8 12;-fx-background-color:#f9fafb;"
                                + "-fx-background-radius:8;-fx-border-color:#f0f0f0;"
                                + "-fx-border-radius:8;");

                        Label lblNom  = new Label(nomAnimal);
                        lblNom.setStyle("-fx-font-weight:bold;-fx-font-size:12px;");
                        lblNom.setPrefWidth(200);

                        Label lblEtat = new Label(s.getEtatSante());
                        lblEtat.setStyle(switch (s.getEtatSante()) {
                            case "Bon"      -> "-fx-text-fill:white;-fx-background-color:#2e7d32;-fx-padding:3 8;-fx-background-radius:10;-fx-font-size:11px;";
                            case "Malade"   -> "-fx-text-fill:white;-fx-background-color:#f57c00;-fx-padding:3 8;-fx-background-radius:10;-fx-font-size:11px;";
                            case "Critique" -> "-fx-text-fill:white;-fx-background-color:#c62828;-fx-padding:3 8;-fx-background-radius:10;-fx-font-size:11px;";
                            default         -> "-fx-font-size:11px;";
                        });

                        Label lblTemp = new Label(s.getTemperature() + " C");
                        lblTemp.setStyle("-fx-font-size:11px;-fx-text-fill:#666;");

                        Label lblDate = new Label(new java.text.SimpleDateFormat("dd/MM/yyyy").format(s.getDateSuivi()));
                        lblDate.setStyle("-fx-font-size:11px;-fx-text-fill:#999;");

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        row.getChildren().addAll(lblNom, lblEtat, lblTemp, spacer, lblDate);
                        activiteBox.getChildren().add(row);
                    });

        } catch (SQLException e) {
            showAlert(e.getMessage());
        }
    }

    @FXML void navigateAnimaux() { naviguer("/fxml/AdminAnimaux.fxml"); }
    @FXML void navigateSuivis()  { naviguer("/fxml/AdminSuivis.fxml");  }

    private void naviguer(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            NavigationUtil.loadInContentArea(lblTotalAnimaux, root);
        } catch (Exception e) { showAlert("Navigation: " + e.getMessage()); }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
