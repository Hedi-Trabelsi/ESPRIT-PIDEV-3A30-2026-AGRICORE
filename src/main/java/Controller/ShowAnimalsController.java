package Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import Model.Animal;
import services.AnimalService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ShowAnimalsController {

    @FXML private FlowPane     animalGrid;
    @FXML private TextField    searchField;
    @FXML private ComboBox<String> triCombo;

    private final AnimalService as = new AnimalService();
    private ObservableList<Animal> observableList;

    @FXML
    void initialize() {
        triCombo.setItems(FXCollections.observableArrayList(
                "Aucun tri", "Espece (A-Z)", "Espece (Z-A)",
                "Race (A-Z)", "Race (Z-A)", "Sexe (A-Z)"));
        triCombo.setValue("Aucun tri");
        triCombo.setOnAction(e -> appliquerTri());
        refreshData();
    }

    private void appliquerTri() {
        if (observableList == null) return;
        List<Animal> liste = FXCollections.observableArrayList(observableList);
        switch (triCombo.getValue()) {
            case "Espece (A-Z)"  -> liste.sort(Comparator.comparing(Animal::getEspece));
            case "Espece (Z-A)"  -> liste.sort(Comparator.comparing(Animal::getEspece).reversed());
            case "Race (A-Z)"    -> liste.sort(Comparator.comparing(Animal::getRace));
            case "Race (Z-A)"    -> liste.sort(Comparator.comparing(Animal::getRace).reversed());
            case "Sexe (A-Z)"    -> liste.sort(Comparator.comparing(Animal::getSexe));
            default -> {}
        }
        afficherCartes(liste);
    }

    @FXML
    private void searchAnimals() {
        String kw = searchField.getText().toLowerCase();
        if (kw.isEmpty()) { afficherCartes(observableList); return; }
        List<Animal> filtered = observableList.stream()
                .filter(a -> a.getCodeAnimal().toLowerCase().contains(kw)
                        || a.getEspece().toLowerCase().contains(kw)
                        || a.getRace().toLowerCase().contains(kw))
                .collect(Collectors.toList());
        afficherCartes(filtered);
    }

    private void refreshData() {
        try {
            observableList = FXCollections.observableArrayList(as.read());
            afficherCartes(observableList);
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private void afficherCartes(List<Animal> animaux) {
        animalGrid.getChildren().clear();
        for (Animal a : animaux) animalGrid.getChildren().add(creerCarte(a));
    }

    private VBox creerCarte(Animal animal) {

        // Emoji selon espèce
        String emoji = switch (animal.getEspece() == null ? "" : animal.getEspece().toLowerCase()) {
            case "vache", "bovin"     -> "🐄";
            case "cheval", "equin"    -> "🐴";
            case "mouton", "ovin"     -> "🐑";
            case "chevre", "caprin"   -> "🐐";
            case "porc", "porcin"     -> "🐷";
            case "poulet", "volaille" -> "🐔";
            case "lapin"              -> "🐰";
            case "chien"              -> "🐕";
            case "chat"               -> "🐈";
            default                   -> "🐾";
        };

        // ── Zone icône ──
        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size:52px;");
        lblEmoji.setMaxWidth(Double.MAX_VALUE);
        lblEmoji.setAlignment(javafx.geometry.Pos.CENTER);

        StackPane iconPane = new StackPane(lblEmoji);
        iconPane.setPrefHeight(110);
        iconPane.setStyle("-fx-background-color:#e8f5e9; -fx-background-radius:14 14 0 0;");

        // ── Code animal ──
        Label lblCode = new Label(animal.getCodeAnimal());
        lblCode.setStyle("-fx-font-size:17px; -fx-font-weight:bold; -fx-text-fill:#1b5e20;");

        // ── Badge espèce ──
        Label lblEspece = new Label(animal.getEspece() != null ? animal.getEspece() : "—");
        lblEspece.setStyle("-fx-background-color:#2e7d32; -fx-text-fill:white; "
                + "-fx-padding:4 12; -fx-background-radius:14; "
                + "-fx-font-size:12px; -fx-font-weight:bold;");

        // ── Race ──
        Label lblRace = new Label("Race : " + (animal.getRace() != null ? animal.getRace() : "—"));
        lblRace.setStyle("-fx-font-size:13px; -fx-text-fill:#388e3c; -fx-font-weight:bold;");

        // ── Sexe + Date ──
        Label lblSexe = new Label("Sexe : " + (animal.getSexe() != null ? animal.getSexe() : "—"));
        lblSexe.setStyle("-fx-font-size:12px; -fx-text-fill:#555;");

        Label lblDate = new Label("Né le : " + (animal.getDateNaissance() != null
                ? animal.getDateNaissance().toString() : "—"));
        lblDate.setStyle("-fx-font-size:12px; -fx-text-fill:#777;");

        VBox infoBox = new VBox(8, lblCode, lblEspece, lblRace, lblSexe, lblDate);
        infoBox.setStyle("-fx-padding:14 16 10 16;");

        // ── Séparateur ──
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#c8e6c9;");

        // ── Boutons ──
        Button btnSuivi = new Button("👁 Suivi");
        btnSuivi.setStyle("-fx-background-color:#1976d2; -fx-text-fill:white; "
                + "-fx-background-radius:8; -fx-padding:7 14; "
                + "-fx-font-size:12px; -fx-font-weight:bold; -fx-cursor:hand;");

        Button btnModifier = new Button("✏ Modifier");
        btnModifier.setStyle("-fx-background-color:#f57c00; -fx-text-fill:white; "
                + "-fx-background-radius:8; -fx-padding:7 14; "
                + "-fx-font-size:12px; -fx-font-weight:bold; -fx-cursor:hand;");

        Button btnSupprimer = new Button("🗑 Supprimer");
        btnSupprimer.setStyle("-fx-background-color:#c62828; -fx-text-fill:white; "
                + "-fx-background-radius:8; -fx-padding:7 14; "
                + "-fx-font-size:12px; -fx-font-weight:bold; -fx-cursor:hand;");

        VBox btnBox = new VBox(8, btnSuivi, btnModifier, btnSupprimer);
        btnBox.setStyle("-fx-padding:10 16 14 16;");
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        btnSuivi.setMaxWidth(Double.MAX_VALUE);
        btnModifier.setMaxWidth(Double.MAX_VALUE);
        btnSupprimer.setMaxWidth(Double.MAX_VALUE);

        // ── Actions ──
        btnSuivi.setOnAction(e -> navigateShowSuiviAnimal());

        btnModifier.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateAnimal.fxml"));
                Parent root = loader.load();
                UpdateAnimalController ctrl = loader.getController();
                ctrl.setAnimal(animal);
                NavigationUtil.loadInContentArea(animalGrid, root);
            } catch (Exception ex) { showAlert(ex.getMessage()); }
        });

        btnSupprimer.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer " + animal.getCodeAnimal() + " ?");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    try { as.delete(animal.getIdAnimal()); refreshData(); }
                    catch (Exception ex) { showAlert(ex.getMessage()); }
                }
            });
        });

        // ── Carte ──
        VBox carte = new VBox(iconPane, infoBox, sep, btnBox);
        carte.setPrefWidth(240);
        carte.setMaxWidth(240);
        carte.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 16;
                -fx-border-radius: 16;
                -fx-border-color: #c8e6c9;
                -fx-border-width: 1.5;
                -fx-effect: dropshadow(gaussian, rgba(0,100,0,0.12), 10, 0, 0, 3);
                """);

        carte.setOnMouseEntered(e -> carte.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 16;
                -fx-border-radius: 16;
                -fx-border-color: #2e7d32;
                -fx-border-width: 2;
                -fx-effect: dropshadow(gaussian, rgba(0,100,0,0.22), 14, 0, 0, 5);
                """));
        carte.setOnMouseExited(e -> carte.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 16;
                -fx-border-radius: 16;
                -fx-border-color: #c8e6c9;
                -fx-border-width: 1.5;
                -fx-effect: dropshadow(gaussian, rgba(0,100,0,0.12), 10, 0, 0, 3);
                """));

        return carte;
    }

    // ── Navigation ──
    @FXML void navigateMeteo() {
        try { NavigationUtil.loadInContentArea(animalGrid,
                FXMLLoader.load(getClass().getResource("/fxml/MeteoSante.fxml")));
        } catch (Exception e) { showAlert(e.getMessage()); }
    }
    @FXML void navigateAddAnimal() {
        try { NavigationUtil.loadInContentArea(animalGrid,
                FXMLLoader.load(getClass().getResource("/fxml/AddAnimal.fxml")));
        } catch (Exception e) { showAlert(e.getMessage()); }
    }
    @FXML void navigateShowSuiviAnimal() {
        try { NavigationUtil.loadInContentArea(animalGrid,
                FXMLLoader.load(getClass().getResource("/fxml/ShowSuiviAnimal.fxml")));
        } catch (Exception e) { showAlert(e.getMessage()); }
    }
    @FXML void navigateStatistiques() {
        try { NavigationUtil.loadInContentArea(animalGrid,
                FXMLLoader.load(getClass().getResource("/fxml/StatistiquesSuivi.fxml")));
        } catch (Exception e) { showAlert(e.getMessage()); }
    }
    @FXML void navigateNutrition() {
        try { NavigationUtil.loadInContentArea(animalGrid,
                FXMLLoader.load(getClass().getResource("/fxml/RecommandationAlimentaire.fxml")));
        } catch (Exception e) { showAlert(e.getMessage()); }
    }
    @FXML void navigateOrdonnance() {
        try { NavigationUtil.loadInContentArea(animalGrid,
                FXMLLoader.load(getClass().getResource("/fxml/OrdonnanceIA.fxml")));
        } catch (Exception e) { showAlert(e.getMessage()); }
    }
    @FXML void navigateResumeMedical() {
        try { NavigationUtil.loadInContentArea(animalGrid,
                FXMLLoader.load(getClass().getResource("/fxml/ResumeMedical.fxml")));
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
}
