package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.Depense;
import models.TypeDepense;
import models.User;
import models.Vente;
import services.DepenseService;
import services.VenteService;
import services.ai.AnomalyDetectionService;
import services.ai.DefaultAnomalyDetectionService;
import models.AnomalyResult;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class UserOperationsController {

    // ── Header ──────────────────────────────────────────────────────────────
    @FXML private Label userTitle;

    // ── Depense form ─────────────────────────────────────────────────────────
    @FXML private ComboBox<TypeDepense> depenseType;
    @FXML private TextField depenseMontant;
    @FXML private TextField depenseDesc;
    @FXML private DatePicker depenseDate;
    @FXML private VBox depenseFormBox;

    // ── Vente form ───────────────────────────────────────────────────────────
    @FXML private TextField ventePrixUnitaire;
    @FXML private TextField venteQuantite;
    @FXML private TextField venteProduit;
    @FXML private Label venteCAValue;
    @FXML private DatePicker venteDate;
    @FXML private VBox venteFormBox;

    // ── Forms wrapper ────────────────────────────────────────────────────────
    @FXML private HBox depenseVenteForms;

    // ── Card panes (replace TableViews) ──────────────────────────────────────
    @FXML private FlowPane depenseCardsPane;
    @FXML private FlowPane venteCardsPane;

    // ── Analytics labels ─────────────────────────────────────────────────────
    @FXML private Label avgDepenseLabel;
    @FXML private Label trendLabel;
    @FXML private Label forecastLabel;
    @FXML private Label recommendationLabel;
    @FXML private Label dep30Label;
    @FXML private Label ven30Label;
    @FXML private Label benef30Label;

    // ── Charts ───────────────────────────────────────────────────────────────
    @FXML private PieChart depenseByTypeChart;
    @FXML private BarChart<String, Number> monthlyBarChart;

    // ── State ────────────────────────────────────────────────────────────────
    private User user;
    private final DepenseService depenseService = new DepenseService();
    private final VenteService venteService = new VenteService();
    private final ObservableList<Depense> depenses = FXCollections.observableArrayList();
    private final ObservableList<Vente> ventes = FXCollections.observableArrayList();
    private Depense editingDepense = null;
    private Vente editingVente = null;

    // ────────────────────────────────────────────────────────────────────────
    @FXML
    void initialize() {
        if (depenseType != null) {
            depenseType.getItems().setAll(TypeDepense.values());
        }
        if (ventePrixUnitaire != null && venteQuantite != null) {
            ventePrixUnitaire.textProperty().addListener((obs, o, n) -> recalcCA());
            venteQuantite.textProperty().addListener((obs, o, n) -> recalcCA());
        }
    }

    // ── User entry point ─────────────────────────────────────────────────────
    public void setUser(User user) {
        this.user = user;
        if (userTitle != null && user != null) {
            userTitle.setText("Utilisateur: " + user.getFirstName() + " " + user.getLastName());
        }
        wireFinanceNav();
        if (depenseType != null && depenseType.getItems().isEmpty()) {
            depenseType.getItems().setAll(TypeDepense.values());
        }
        loadDepenses();
        loadVentes();
    }

    // ── Navigation ───────────────────────────────────────────────────────────
    private void wireFinanceNav() {
        Platform.runLater(() -> {
            try {
                Button btn = (Button) userTitle.getScene().getRoot().lookup("#financeBtn");
                if (btn != null) btn.setOnAction(e -> openFinancePage());
            } catch (Exception ignored) {}
        });
    }

    private void openFinancePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FinanceTables.fxml"));
            Parent root = loader.load();
            controllers.FinanceTablesController controller = loader.getController();
            controller.setUser(user);
            userTitle.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation finance", e.getMessage());
        }
    }

    @FXML
    void goBack() {
        try {
            Parent root = new FXMLLoader(getClass().getResource("/ShowUsers.fxml")).load();
            userTitle.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Erreur navigation", e.getMessage());
        }
    }

    // ── Show forms (modal windows) ───────────────────────────────────────────
    @FXML
    void showDepenseForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DepenseForm.fxml"));
            Parent root = loader.load();
            controllers.DepenseFormController controller = loader.getController();
            controller.setUser(user);
            controller.setOnSaved(v -> loadDepenses());
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Ajouter Dépense");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            showError("Formulaire dépense", e.getMessage());
        }
    }

    @FXML
    void showVenteForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VenteForm.fxml"));
            Parent root = loader.load();
            controllers.VenteFormController controller = loader.getController();
            controller.setUser(user);
            controller.setOnSaved(v -> loadVentes());
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Ajouter Vente");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            showError("Formulaire vente", e.getMessage());
        }
    }

    // ── Save depense ─────────────────────────────────────────────────────────
    @FXML
    void saveDepense() {
        try {
            TypeDepense type = depenseType.getValue();
            double montant = Double.parseDouble(depenseMontant.getText());
            LocalDate date = depenseDate.getValue();
            if (editingDepense != null) {
                editingDepense.setType(type);
                editingDepense.setMontant(montant);
                editingDepense.setDate(date);
                depenseService.update(editingDepense);
                showInfo("Dépense mise à jour");
            } else {
                Depense d = new Depense(0, user.getId(), montant, null, date);
                d.setType(type);
                AnomalyDetectionService ads = new services.ai.RobustAnomalyDetectionService();
                AnomalyResult res = ads.analyzeDepense(user.getId(), depenses, d);
                if (res.isAnomaly()) {
                    Alert warn = new Alert(Alert.AlertType.WARNING);
                    warn.setTitle("Dépense anormale");
                    warn.setHeaderText("Dépense potentiellement anormale");
                    String bounds = "";
                    if (res.getLowerBound() != null && res.getUpperBound() != null) {
                        bounds = " Intervalle attendu: [" + String.format("%.2f", res.getLowerBound())
                                + " ; " + String.format("%.2f", res.getUpperBound()) + "].";
                    }
                    warn.setContentText("Score: " + String.format("%.2f", res.getScore()) + "." + bounds);
                    try {
                        DialogPane pane = warn.getDialogPane();
                        var css = getClass().getResource("/design.css");
                        if (css != null) {
                            pane.getStylesheets().add(css.toExternalForm());
                        }
                        pane.getStyleClass().addAll("modern-alert", "warning");
                        Label icon = new Label("⚠");
                        icon.getStyleClass().addAll("alert-icon", "warning");
                        warn.setGraphic(icon);
                    } catch (Exception ignored) {}
                    warn.showAndWait();
                }
                depenseService.create(d);
                showInfo("Dépense enregistrée");
            }
            loadDepenses();
            clearDepenseForm();
        } catch (Exception e) {
            showError("Erreur d'enregistrement dépense", e.getMessage());
        }
    }

    // ── Save vente ───────────────────────────────────────────────────────────
    @FXML
    void saveVente() {
        try {
            double prixUnitaire = Double.parseDouble(ventePrixUnitaire.getText());
            double quantite = Double.parseDouble(venteQuantite.getText());
            String produit = venteProduit.getText();
            LocalDate date = venteDate.getValue();
            double ca = prixUnitaire * quantite;
            if (venteCAValue != null) venteCAValue.setText(String.valueOf(ca));
            if (editingVente != null) {
                editingVente.setPrixUnitaire(prixUnitaire);
                editingVente.setQuantite(quantite);
                editingVente.setChiffreAffaires(ca);
                editingVente.setDate(date);
                editingVente.setProduit(produit);
                venteService.update(editingVente);
                showInfo("Vente mise à jour");
            } else {
                Vente v = new Vente(0, user.getId(), null, prixUnitaire, quantite, ca, date, produit, null);
                venteService.create(v);
                showInfo("Vente enregistrée");
            }
            loadVentes();
            clearVenteForm();
        } catch (Exception e) {
            showError("Erreur d'enregistrement vente", e.getMessage());
        }
    }

    // ── Load & render cards ──────────────────────────────────────────────────
    private void loadDepenses() {
        if (user == null || depenseCardsPane == null) return;
        try {
            depenses.setAll(depenseService.readByUser(user.getId()));
        } catch (Exception e) {
            showError("Chargement dépenses", e.getMessage());
        }
        renderDepenseCards();
    }

    private void loadVentes() {
        if (user == null || venteCardsPane == null) return;
        try {
            ventes.setAll(venteService.readByUser(user.getId()));
        } catch (Exception e) {
            showError("Chargement ventes", e.getMessage());
        }
        renderVenteCards();
    }

    private void renderDepenseCards() {
        depenseCardsPane.getChildren().clear();
        if (depenses.isEmpty()) {
            Label empty = new Label("Aucune dépense enregistrée.");
            empty.getStyleClass().add("empty-state");
            depenseCardsPane.getChildren().add(empty);
            return;
        }
        for (Depense d : depenses) {
            depenseCardsPane.getChildren().add(buildDepenseCard(d));
        }
    }

    private void renderVenteCards() {
        venteCardsPane.getChildren().clear();
        if (ventes.isEmpty()) {
            Label empty = new Label("Aucune vente enregistrée.");
            empty.getStyleClass().add("empty-state");
            venteCardsPane.getChildren().add(empty);
            return;
        }
        for (Vente v : ventes) {
            venteCardsPane.getChildren().add(buildVenteCard(v));
        }
    }

    // ── Card builders ────────────────────────────────────────────────────────
    private VBox buildDepenseCard(Depense d) {
        VBox card = new VBox(6);
        card.getStyleClass().add("item-card");

        // Badge: type
        Label badge = new Label(d.getType() != null ? d.getType().name() : "AUTRE");
        badge.getStyleClass().add("item-card-badge");

        // Amount
        Label amount = new Label(String.format("%.2f DT", d.getMontant()));
        amount.getStyleClass().add("item-card-amount");

        // Description
        String descText = (d.getDescription() != null && !d.getDescription().isBlank())
                ? d.getDescription() : "—";
        Label desc = new Label(descText);
        desc.getStyleClass().add("item-card-desc");
        desc.setWrapText(true);

        // Date
        Label date = new Label("📅 " + (d.getDate() != null ? d.getDate().toString() : "—"));
        date.getStyleClass().add("item-card-meta");

        // Actions
        Button edit = new Button("✎ Modifier");
        edit.getStyleClass().add("btn-icon");
        edit.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/DepenseForm.fxml"));
                Parent root = loader.load();
                controllers.DepenseFormController controller = loader.getController();
                controller.setUser(user);
                controller.setEditing(d);
                controller.setOnSaved(v -> loadDepenses());
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle("Modifier Dépense");
                stage.setScene(new javafx.scene.Scene(root));
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.showAndWait();
            } catch (Exception ex) {
                showError("Édition dépense", ex.getMessage());
            }
        });

        Button del = new Button("✕ Supprimer");
        del.getStyleClass().addAll("btn-icon", "danger");
        del.setOnAction(e -> {
            try {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la suppression de cette dépense ?", ButtonType.CANCEL, ButtonType.OK);
                confirm.setTitle("Supprimer Dépense");
                confirm.setHeaderText("Cette action est irréversible");
                try {
                    DialogPane pane = confirm.getDialogPane();
                    var css = getClass().getResource("/design.css");
                    if (css != null) {
                        pane.getStylesheets().add(css.toExternalForm());
                    }
                    pane.getStyleClass().addAll("modern-alert", "danger");
                    Label icon = new Label("🗑");
                    icon.getStyleClass().addAll("alert-icon", "danger");
                    confirm.setGraphic(icon);
                } catch (Exception ignored) {}
                var result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    depenseService.delete(d.getIdDepense());
                    loadDepenses();
                }
            } catch (Exception ex) {
                showError("Suppression dépense", ex.getMessage());
            }
        });

        HBox actions = new HBox(6, edit, del);
        actions.getStyleClass().add("item-card-actions");

        card.getChildren().addAll(badge, amount, desc, date, actions);
        return card;
    }

    private VBox buildVenteCard(Vente v) {
        VBox card = new VBox(6);
        card.getStyleClass().add("item-card");

        // Badge: product
        Label badge = new Label(v.getProduit() != null ? v.getProduit() : "Produit");
        badge.getStyleClass().add("item-card-badge");

        // CA as main figure
        Label amount = new Label(String.format("%.2f DT", v.getChiffreAffaires()));
        amount.getStyleClass().add("item-card-amount");

        // Price × qty
        Label meta = new Label(String.format("%.2f × %.0f unités", v.getPrixUnitaire(), v.getQuantite()));
        meta.getStyleClass().add("item-card-desc");

        // Date
        Label date = new Label("📅 " + (v.getDate() != null ? v.getDate().toString() : "—"));
        date.getStyleClass().add("item-card-meta");

        // Actions
        Button edit = new Button("✎ Modifier");
        edit.getStyleClass().add("btn-icon");
        edit.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/VenteForm.fxml"));
                Parent root = loader.load();
                controllers.VenteFormController controller = loader.getController();
                controller.setUser(user);
                controller.setEditing(v);
                controller.setOnSaved(x -> loadVentes());
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle("Modifier Vente");
                stage.setScene(new javafx.scene.Scene(root));
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.showAndWait();
            } catch (Exception ex) {
                showError("Édition vente", ex.getMessage());
            }
        });

        Button del = new Button("✕ Supprimer");
        del.getStyleClass().addAll("btn-icon", "danger");
        del.setOnAction(e -> {
            try {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la suppression de cette vente ?", ButtonType.CANCEL, ButtonType.OK);
                confirm.setTitle("Supprimer Vente");
                confirm.setHeaderText("Cette action est irréversible");
                try {
                    DialogPane pane = confirm.getDialogPane();
                    var css = getClass().getResource("/design.css");
                    if (css != null) {
                        pane.getStylesheets().add(css.toExternalForm());
                    }
                    pane.getStyleClass().addAll("modern-alert", "danger");
                    Label icon = new Label("🗑");
                    icon.getStyleClass().addAll("alert-icon", "danger");
                    confirm.setGraphic(icon);
                } catch (Exception ignored) {}
                var result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    venteService.delete(v.getIdVente());
                    loadVentes();
                }
            } catch (Exception ex) {
                showError("Suppression vente", ex.getMessage());
            }
        });

        HBox actions = new HBox(6, edit, del);
        actions.getStyleClass().add("item-card-actions");

        card.getChildren().addAll(badge, amount, meta, date, actions);
        return card;
    }

    // ── Form helpers ─────────────────────────────────────────────────────────
    private void clearDepenseForm() {
        if (depenseType != null) depenseType.getSelectionModel().clearSelection();
        depenseMontant.clear();
        depenseDesc.clear();
        depenseDate.setValue(null);
        editingDepense = null;
    }

    private void clearVenteForm() {
        ventePrixUnitaire.clear();
        venteQuantite.clear();
        venteProduit.clear();
        if (venteCAValue != null) venteCAValue.setText("0.0");
        venteDate.setValue(null);
        editingVente = null;
    }

    private void recalcCA() {
        try {
            double p = Double.parseDouble(ventePrixUnitaire.getText());
            double q = Double.parseDouble(venteQuantite.getText());
            if (venteCAValue != null) venteCAValue.setText(String.valueOf(p * q));
        } catch (Exception ignored) {
            if (venteCAValue != null) venteCAValue.setText("0.0");
        }
    }

    // ── Analytics ────────────────────────────────────────────────────────────
    @FXML
    void refreshAnalytics() {
        try {
            double avg = depenses.stream().mapToDouble(Depense::getMontant).average().orElse(0.0);
            if (avgDepenseLabel != null) avgDepenseLabel.setText(String.format("%.2f", avg));

            Map<YearMonth, Double> monthly = new HashMap<>();
            for (Depense d : depenses) {
                if (d.getDate() == null) continue;
                YearMonth ym = YearMonth.from(d.getDate());
                monthly.put(ym, monthly.getOrDefault(ym, 0.0) + d.getMontant());
            }
            List<Map.Entry<YearMonth, Double>> sorted = monthly.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                    .collect(Collectors.toList());
            double m0 = sorted.size() > 0 ? sorted.get(0).getValue() : 0.0;
            double m1 = sorted.size() > 1 ? sorted.get(1).getValue() : 0.0;
            double m2 = sorted.size() > 2 ? sorted.get(2).getValue() : 0.0;
            double wma = (3 * m0 + 2 * m1 + m2) / 6.0;
            double trend = m1 > 0 ? (m0 - m1) / m1 : 0.0;
            double trendClamped = Math.max(-0.5, Math.min(0.5, trend));
            double forecast = wma * (1 + trendClamped);
            if (trendLabel != null) trendLabel.setText(String.format("%.0f%%", trendClamped * 100));
            if (forecastLabel != null) forecastLabel.setText(String.format("%.2f", forecast));

            LocalDate now = LocalDate.now();
            LocalDate cutoff = now.minusDays(30);
            double dep30 = depenses.stream()
                    .filter(d -> d.getDate() != null && !d.getDate().isBefore(cutoff))
                    .mapToDouble(Depense::getMontant).sum();
            double ven30 = ventes.stream()
                    .filter(v -> v.getDate() != null && !v.getDate().isBefore(cutoff))
                    .mapToDouble(Vente::getChiffreAffaires).sum();
            if (dep30Label != null) dep30Label.setText(String.format("%.2f", dep30));
            if (ven30Label != null) ven30Label.setText(String.format("%.2f", ven30));
            if (benef30Label != null) benef30Label.setText(String.format("%.2f", (ven30 - dep30)));

            String reco;
            if (dep30 > ven30) {
                reco = "Trop de dépenses: réduis coûts variables ou décale dépenses non urgentes.";
            } else if (trend > 0.2) {
                reco = "Dépenses en hausse: négocie fournisseurs ou plafonne certains budgets.";
            } else {
                reco = "Situation stable: maintenir le suivi et optimiser les achats.";
            }
            if (recommendationLabel != null) recommendationLabel.setText(reco);

            if (depenseByTypeChart != null) {
                Map<String, Double> byType = new HashMap<>();
                for (Depense d : depenses) {
                    String key = d.getType() != null ? d.getType().name() : "AUTRE";
                    byType.put(key, byType.getOrDefault(key, 0.0) + d.getMontant());
                }
                depenseByTypeChart.getData().clear();
                for (Map.Entry<String, Double> e : byType.entrySet()) {
                    depenseByTypeChart.getData().add(new PieChart.Data(e.getKey(), e.getValue()));
                }
            }

            if (monthlyBarChart != null) {
                Map<YearMonth, Double> depMonth = new HashMap<>();
                for (Depense d : depenses) {
                    if (d.getDate() == null) continue;
                    YearMonth ym = YearMonth.from(d.getDate());
                    depMonth.put(ym, depMonth.getOrDefault(ym, 0.0) + d.getMontant());
                }
                Map<YearMonth, Double> venMonth = new HashMap<>();
                for (Vente v : ventes) {
                    if (v.getDate() == null) continue;
                    YearMonth ym = YearMonth.from(v.getDate());
                    venMonth.put(ym, venMonth.getOrDefault(ym, 0.0) + v.getChiffreAffaires());
                }
                List<YearMonth> months = new ArrayList<>(depMonth.keySet());
                for (YearMonth ym : venMonth.keySet()) if (!months.contains(ym)) months.add(ym);
                months.sort(Comparator.naturalOrder());

                XYChart.Series<String, Number> depSeries = new XYChart.Series<>();
                depSeries.setName("Dépenses");
                XYChart.Series<String, Number> venSeries = new XYChart.Series<>();
                venSeries.setName("Ventes");
                for (YearMonth ym : months) {
                    String label = ym.toString();
                    depSeries.getData().add(new XYChart.Data<>(label, depMonth.getOrDefault(ym, 0.0)));
                    venSeries.getData().add(new XYChart.Data<>(label, venMonth.getOrDefault(ym, 0.0)));
                }
                monthlyBarChart.getData().clear();
                monthlyBarChart.getData().addAll(depSeries, venSeries);
            }
        } catch (Exception e) {
            if (recommendationLabel != null) recommendationLabel.setText("Analyse indisponible");
        }
    }

    // ── Alerts ───────────────────────────────────────────────────────────────
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setContentText(content);
        alert.showAndWait();
    }
}
