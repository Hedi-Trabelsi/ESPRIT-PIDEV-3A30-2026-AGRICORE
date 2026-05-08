package Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import Model.Animal;
import Model.SuiviAnimal;
import services.AnimalService;
import services.SuiviAnimalService;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ShowSuiviAnimalController {

    @FXML private FlowPane     suiviGrid;
    @FXML private TextField    searchField;
    @FXML private ComboBox<String> triCombo;

    private final SuiviAnimalService suiviService  = new SuiviAnimalService();
    private final AnimalService      animalService = new AnimalService();

    private ObservableList<SuiviAnimal> suiviList = FXCollections.observableArrayList();
    private List<Animal> animals;

    @FXML
    public void initialize() {
        try { animals = animalService.read(); }
        catch (SQLException e) { e.printStackTrace(); }

        triCombo.setItems(FXCollections.observableArrayList(
                "Aucun tri", "Date (Plus recent)", "Date (Plus ancien)",
                "Temperature (Croissant)", "Temperature (Decroissant)",
                "Poids (Croissant)", "Poids (Decroissant)", "Etat de sante"));
        triCombo.setValue("Aucun tri");
        triCombo.setOnAction(e -> appliquerTri());

        loadData();
        setupSearch();
    }

    private void appliquerTri() {
        List<SuiviAnimal> liste = FXCollections.observableArrayList(suiviList);
        switch (triCombo.getValue()) {
            case "Date (Plus recent)"        -> liste.sort(Comparator.comparing(SuiviAnimal::getDateSuivi).reversed());
            case "Date (Plus ancien)"        -> liste.sort(Comparator.comparing(SuiviAnimal::getDateSuivi));
            case "Temperature (Croissant)"   -> liste.sort(Comparator.comparingDouble(SuiviAnimal::getTemperature));
            case "Temperature (Decroissant)" -> liste.sort(Comparator.comparingDouble(SuiviAnimal::getTemperature).reversed());
            case "Poids (Croissant)"         -> liste.sort(Comparator.comparingDouble(SuiviAnimal::getPoids));
            case "Poids (Decroissant)"       -> liste.sort(Comparator.comparingDouble(SuiviAnimal::getPoids).reversed());
            case "Etat de sante"             -> liste.sort(Comparator.comparing(SuiviAnimal::getEtatSante));
            default -> {}
        }
        afficherCartes(liste);
    }

    private void loadData() {
        try {
            suiviList.clear();
            suiviList.addAll(suiviService.read());
            afficherCartes(suiviList);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) { afficherCartes(suiviList); return; }
            String kw = newVal.toLowerCase();
            List<SuiviAnimal> filtered = suiviList.stream().filter(s -> {
                String niveau   = s.getNiveauActivite() == null ? "" : s.getNiveauActivite().toLowerCase();
                String etat     = s.getEtatSante()      == null ? "" : s.getEtatSante().toLowerCase();
                String remarque = s.getRemarque()        == null ? "" : s.getRemarque().toLowerCase();
                return niveau.contains(kw) || etat.contains(kw) || remarque.contains(kw)
                        || String.valueOf(s.getTemperature()).contains(kw)
                        || String.valueOf(s.getPoids()).contains(kw);
            }).collect(Collectors.toList());
            afficherCartes(filtered);
        });
    }

    private void afficherCartes(List<SuiviAnimal> suivis) {
        suiviGrid.getChildren().clear();
        for (SuiviAnimal s : suivis) suiviGrid.getChildren().add(creerCarte(s));
    }

    private VBox creerCarte(SuiviAnimal s) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        // Nom animal
        String nomAnimal = animals == null ? "Inconnu" : animals.stream()
                .filter(a -> a.getIdAnimal() == s.getIdAnimal())
                .map(a -> a.getCodeAnimal() + " — " + a.getEspece())
                .findFirst().orElse("Inconnu");

        // Couleur selon état
        String etat = s.getEtatSante() != null ? s.getEtatSante() : "—";
        String couleurEtat, bgEtat, iconEtat;
        switch (etat) {
            case "Bon"      -> { couleurEtat = "#2e7d32"; bgEtat = "#e8f5e9"; iconEtat = "✅"; }
            case "Malade"   -> { couleurEtat = "#e65100"; bgEtat = "#fff3e0"; iconEtat = "⚠️"; }
            case "Critique" -> { couleurEtat = "#c62828"; bgEtat = "#ffebee"; iconEtat = "🔴"; }
            default         -> { couleurEtat = "#555";    bgEtat = "#f5f5f5"; iconEtat = "❓"; }
        }

        // ── Zone header ──
        Label lblIcon = new Label(iconEtat);
        lblIcon.setStyle("-fx-font-size:36px;");
        lblIcon.setMaxWidth(Double.MAX_VALUE);
        lblIcon.setAlignment(javafx.geometry.Pos.CENTER);

        StackPane headerPane = new StackPane(lblIcon);
        headerPane.setPrefHeight(80);
        headerPane.setStyle("-fx-background-color:" + bgEtat + "; -fx-background-radius:14 14 0 0;");

        // ── Nom animal ──
        Label lblAnimal = new Label(nomAnimal);
        lblAnimal.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1b5e20;");
        lblAnimal.setWrapText(true);

        // ── Date ──
        Label lblDate = new Label("📅 " + sdf.format(s.getDateSuivi()));
        lblDate.setStyle("-fx-font-size:11px; -fx-text-fill:#777;");

        // ── Badge état ──
        Label lblEtat = new Label(iconEtat + " " + etat);
        lblEtat.setStyle("-fx-background-color:" + couleurEtat + "; -fx-text-fill:white; "
                + "-fx-padding:4 12; -fx-background-radius:14; "
                + "-fx-font-size:12px; -fx-font-weight:bold;");

        // ── Constantes vitales ──
        Label lblTemp = new Label("🌡️ " + s.getTemperature() + " °C");
        lblTemp.setStyle("-fx-font-size:12px; -fx-text-fill:#333; -fx-font-weight:bold;");

        Label lblPoids = new Label("⚖️ " + s.getPoids() + " kg");
        lblPoids.setStyle("-fx-font-size:12px; -fx-text-fill:#333; -fx-font-weight:bold;");

        Label lblRythme = new Label("❤️ " + s.getRythmeCardiaque() + " bpm");
        lblRythme.setStyle("-fx-font-size:12px; -fx-text-fill:#333; -fx-font-weight:bold;");

        Label lblNiveau = new Label("🏃 Activite : " + (s.getNiveauActivite() != null ? s.getNiveauActivite() : "—"));
        lblNiveau.setStyle("-fx-font-size:12px; -fx-text-fill:#555;");

        // ── Remarque ──
        String rem = s.getRemarque() != null && !s.getRemarque().isEmpty()
                ? s.getRemarque() : "Aucune remarque";
        Label lblRem = new Label("📝 " + rem);
        lblRem.setStyle("-fx-font-size:11px; -fx-text-fill:#888;");
        lblRem.setWrapText(true);

        HBox vitaux = new HBox(10, lblTemp, lblPoids, lblRythme);
        vitaux.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox infoBox = new VBox(7, lblAnimal, lblDate, lblEtat, vitaux, lblNiveau, lblRem);
        infoBox.setStyle("-fx-padding:12 16 8 16;");

        // ── Séparateur ──
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#c8e6c9;");

        // ── Boutons ──
        Button btnModifier = new Button("✏ Modifier");
        btnModifier.setStyle("-fx-background-color:#f57c00; -fx-text-fill:white; "
                + "-fx-background-radius:8; -fx-padding:8 14; "
                + "-fx-font-size:12px; -fx-font-weight:bold; -fx-cursor:hand;");

        Button btnSupprimer = new Button("🗑 Supprimer");
        btnSupprimer.setStyle("-fx-background-color:#c62828; -fx-text-fill:white; "
                + "-fx-background-radius:8; -fx-padding:8 14; "
                + "-fx-font-size:12px; -fx-font-weight:bold; -fx-cursor:hand;");

        btnModifier.setMaxWidth(Double.MAX_VALUE);
        btnSupprimer.setMaxWidth(Double.MAX_VALUE);

        VBox btnBox = new VBox(8, btnModifier, btnSupprimer);
        btnBox.setStyle("-fx-padding:10 16 14 16;");

        // ── Actions ──
        btnModifier.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateSuiviAnimal.fxml"));
                Parent root = loader.load();
                UpdateSuiviAnimalController ctrl = loader.getController();
                ctrl.setSuivi(s);
                NavigationUtil.loadInContentArea(suiviGrid, root);
            } catch (IOException ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).show(); }
        });

        btnSupprimer.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce suivi ?");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    try { suiviService.delete(s.getIdSuivi()); loadData(); }
                    catch (SQLException ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).show(); }
                }
            });
        });

        // ── Carte ──
        String borderColor = etat.equals("Critique") ? "#c62828"
                : etat.equals("Malade") ? "#e65100" : "#c8e6c9";

        VBox carte = new VBox(headerPane, infoBox, sep, btnBox);
        carte.setPrefWidth(260);
        carte.setMaxWidth(260);
        carte.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 16;
                -fx-border-radius: 16;
                -fx-border-color: %s;
                -fx-border-width: 1.5;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3);
                """.formatted(borderColor));

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
                -fx-border-color: %s;
                -fx-border-width: 1.5;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3);
                """.formatted(borderColor)));

        return carte;
    }

    // ── Navigation ──
    @FXML private void navigateAddSuivi() {
        try { NavigationUtil.loadInContentArea(suiviGrid,
                FXMLLoader.load(getClass().getResource("/fxml/AddSuiviAnimal.fxml")));
        } catch (IOException e) { new Alert(Alert.AlertType.ERROR, e.getMessage()).show(); }
    }

    @FXML private void navigateStatistiques() {
        try { NavigationUtil.loadInContentArea(suiviGrid,
                FXMLLoader.load(getClass().getResource("/fxml/StatistiquesSuivi.fxml")));
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void navigateBack() {
        try { NavigationUtil.loadInContentArea(suiviGrid,
                FXMLLoader.load(getClass().getResource("/fxml/ShowAnimals.fxml")));
        } catch (IOException e) { e.printStackTrace(); }
    }
}
