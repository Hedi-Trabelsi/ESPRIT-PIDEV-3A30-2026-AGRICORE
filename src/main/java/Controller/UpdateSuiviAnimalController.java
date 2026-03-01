package Controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import Model.SuiviAnimal;
import services.SuiviAnimalService;

public class UpdateSuiviAnimalController {

    @FXML private TextField tempTf;
    @FXML private TextField poidsTf;
    @FXML private TextField rythmeTf;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private ComboBox<String> etatCombo;
    @FXML private TextArea remarqueTf;

    private SuiviAnimal suivi;
    private final SuiviAnimalService service = new SuiviAnimalService();

    @FXML
    void initialize() {
        niveauCombo.setItems(FXCollections.observableArrayList("Faible", "Moyen", "Eleve"));
        etatCombo.setItems(FXCollections.observableArrayList("Bon", "Malade", "Critique"));
    }

    public void setSuivi(SuiviAnimal s) {
        this.suivi = s;
        tempTf.setText(String.valueOf(s.getTemperature()));
        poidsTf.setText(String.valueOf(s.getPoids()));
        rythmeTf.setText(String.valueOf(s.getRythmeCardiaque()));
        niveauCombo.setValue(s.getNiveauActivite());
        etatCombo.setValue(s.getEtatSante());
        remarqueTf.setText(s.getRemarque());
    }

    @FXML
    void updateSuivi(ActionEvent event) {
        if (tempTf.getText().trim().isEmpty()) {
            showError("Temperature obligatoire !");
            return;
        }
        if (poidsTf.getText().trim().isEmpty()) {
            showError("Poids obligatoire !");
            return;
        }
        if (rythmeTf.getText().trim().isEmpty()) {
            showError("Rythme Cardiaque obligatoire !");
            return;
        }
        if (niveauCombo.getValue() == null) {
            showError("Veuillez choisir un niveau d'activite !");
            return;
        }
        if (etatCombo.getValue() == null) {
            showError("Veuillez choisir un etat de sante !");
            return;
        }

        try {
            suivi.setTemperature(Double.parseDouble(tempTf.getText().trim().replace(",", ".")));
            suivi.setPoids(Double.parseDouble(poidsTf.getText().trim().replace(",", ".")));
            suivi.setRythmeCardiaque(Integer.parseInt(rythmeTf.getText().trim()));
            suivi.setNiveauActivite(niveauCombo.getValue());
            suivi.setEtatSante(etatCombo.getValue());
            suivi.setRemarque(remarqueTf.getText());

            service.update(suivi);

            new Alert(Alert.AlertType.INFORMATION, "Suivi modifie avec succes !").showAndWait();
            navigateShowSuiviAnimal();

        } catch (NumberFormatException e) {
            showError("Valeur numerique invalide !\nTemperature ex: 38.5 | Poids ex: 450 | Rythme ex: 70");
        } catch (Exception e) {
            showError("Erreur lors de la modification :\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void navigateShowSuiviAnimal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShowSuiviAnimal.fxml"));
            Parent root = loader.load();
            NavigationUtil.loadInContentArea(tempTf, root);
        } catch (Exception e) {
            showError("Erreur de navigation :\n" + e.getMessage());
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
