package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import Model.Depense;
import Model.TypeDepense;
import Model.User;
import services.DepenseService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

public class DepenseFormController {
    @FXML
    private ComboBox<TypeDepense> typeField;
    @FXML
    private TextField montantField;
    @FXML
    private TextField descField;
    @FXML
    private DatePicker dateField;
    @FXML
    private Label typeError;
    @FXML
    private Label montantError;
    @FXML
    private Label dateError;
    @FXML
    private ComboBox<String> sensitivityField;

    private final DepenseService depenseService;

    {
        try {
            depenseService = new DepenseService();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User user;
    private Depense editing;
    private Consumer<Void> onSaved;
    @FXML
    private Button saveBtn;

    @FXML
    void initialize() {
        typeField.getItems().setAll(TypeDepense.values());
        if (saveBtn != null) saveBtn.setDisable(true);
        typeField.valueProperty().addListener((o,a,b)->validateLive());
        montantField.textProperty().addListener((o,a,b)->validateLive());
        dateField.valueProperty().addListener((o,a,b)->validateLive());
        if (sensitivityField != null) {
            sensitivityField.getItems().setAll("Faible", "Normale", "Forte");
            sensitivityField.getSelectionModel().select("Normale");
        }
    }

    public void setUser(User user) {
        this.user = user;
        System.out.println("[DEBUG] DepenseForm - setUser called with id=" + (user != null ? user.getId() : "null") + " name=" + (user != null ? user.getPrenom() + " " + user.getNom() : "null"));
    }

    public void setEditing(Depense d) {
        this.editing = d;
        if (d != null) {
            typeField.setValue(d.getType());
            montantField.setText(String.valueOf(d.getMontant()));
            descField.setText(d.getDescription() != null ? d.getDescription() : "");
            dateField.setValue(d.getDate());
        }
    }

    public void setOnSaved(Consumer<Void> onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    void save() {
        clearErrors();
        boolean valid = true;
        TypeDepense type = typeField.getValue();
        if (type == null) {
            typeError.setText("Type requis");
            valid = false;
        }
        double montant = 0.0;
        try {
            montant = Double.parseDouble(montantField.getText());
            if (montant <= 0) {
                montantError.setText("Montant > 0 requis");
                valid = false;
            }
        } catch (Exception e) {
            montantError.setText("Montant numérique requis");
            valid = false;
        }
        LocalDate date = dateField.getValue();
        if (date == null) {
            dateError.setText("Date requise");
            valid = false;
        }
        if (!valid) return;

        try {
            if (user == null || user.getId() == 0) {
                new Alert(Alert.AlertType.ERROR, "Erreur: utilisateur non défini (id=0). Impossible d'enregistrer.").showAndWait();
                return;
            }
            System.out.println("[DEBUG] DepenseForm.save - userId=" + user.getId());
            if (editing != null) {
                editing.setType(type);
                editing.setMontant(montant);
                editing.setDescription(descField.getText());
                editing.setDate(date);
                runAnomalyCheck(montant, date, type);
                depenseService.update(editing);
            } else {
                Depense d = new Depense(0, user.getId(), montant, descField.getText(), date);
                d.setType(type);
                runAnomalyCheck(montant, date, type);
                depenseService.create(d);
            }
            if (onSaved != null) onSaved.accept(null);
            closeStage();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    void cancel() {
        closeStage();
    }

    private void clearErrors() {
        typeError.setText(" ");
        montantError.setText(" ");
        dateError.setText(" ");
    }

    private void closeStage() {
        Stage s = (Stage) typeField.getScene().getWindow();
        s.close();
    }

    private void runAnomalyCheck(double montant, LocalDate date, TypeDepense type) throws Exception {
        List<Depense> history = depenseService.readByUser(user.getId());
        Depense cand = new Depense(0, user.getId(), montant, null, date);
        cand.setType(type);
        double thr = 3.5;
        String sel = sensitivityField != null ? sensitivityField.getSelectionModel().getSelectedItem() : "Normale";
        if ("Faible".equals(sel)) thr = 3.0;
        else if ("Forte".equals(sel)) thr = 4.0;
        services.ai.RobustAnomalyDetectionService svc = new services.ai.RobustAnomalyDetectionService(thr);
        Model.AnomalyResult res = svc.analyzeDepense(user.getId(), history, cand);
        if (res.isAnomaly()) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("Dépense anormale");
            warn.setHeaderText("Dépense potentiellement anormale");
            String bounds = "";
            if (res.getLowerBound() != null && res.getUpperBound() != null) {
                bounds = " Intervalle attendu: [" + String.format("%.2f", res.getLowerBound()) + " ; " + String.format("%.2f", res.getUpperBound()) + "].";
            }
            warn.setContentText("Score (robuste): " + String.format("%.2f", res.getScore()) + "." + bounds);
            try {
                DialogPane pane = warn.getDialogPane();
                var css = getClass().getResource("/fxml/shadcn.css");
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
    }


    private void validateLive() {
        clearErrors();
        boolean valid = true;
        if (typeField.getValue() == null) {
            typeError.setText("Type requis");
            valid = false;
        }
        try {
            double m = Double.parseDouble(montantField.getText());
            if (m <= 0) {
                montantError.setText("Montant > 0 requis");
                valid = false;
            }
        } catch (Exception e) {
            montantError.setText("Montant numérique requis");
            valid = false;
        }
        if (dateField.getValue() == null) {
            dateError.setText("Date requise");
            valid = false;
        }
        if (saveBtn != null) saveBtn.setDisable(!valid);
    }
}
