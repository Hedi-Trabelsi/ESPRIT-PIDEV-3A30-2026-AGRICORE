package org.example;
import org.example.UpdateUserController;
import Model.Utilisateur;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    @FXML private TableView<Utilisateur> userTable;
    @FXML private TableColumn<Utilisateur, Integer> colId; // hidden
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colPrenom;
    @FXML private TableColumn<Utilisateur, String> colEmail;
    @FXML private TableColumn<Utilisateur, String> colRole;
    @FXML private TableColumn<Utilisateur, String> colGenre;
    @FXML private TableColumn<Utilisateur, Void> colActions;

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Label statusLabel;
    @FXML private Button logoutButton;

    private ObservableList<Utilisateur> userList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Setup columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(cellData -> {
            int role = cellData.getValue().getRole();
            String roleString = (role == 0) ? "Admin" : "User";
            return new javafx.beans.property.SimpleStringProperty(roleString);
        });
        colGenre.setCellValueFactory(new PropertyValueFactory<>("genre"));

        // Hide ID column
        colId.setVisible(false);

        // Load users from DB
        loadUsers();

        // Add Update/Delete buttons
        addActionButtons();

        // Search functionality
        searchButton.setOnAction(e -> filterUsers());

        // Logout action
        logoutButton.setOnAction(e -> statusLabel.setText("Logged out successfully!"));
    }

    private void loadUsers() {
        userList.clear();
        String sql = "SELECT * FROM user";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Utilisateur user = new Utilisateur();
                user.setId(rs.getInt("id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setDateNaissance(rs.getDate("date").toLocalDate()); // convert SQL date to LocalDate
                user.setAdresse(rs.getString("adresse"));
                user.setPhone(rs.getInt("numeroT"));
                user.setRole(rs.getInt("role"));
                user.setGenre(rs.getString("genre"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));

                userList.add(user);
            }

            userTable.setItems(userList);

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed to load users!");
        }
    }

    private void addActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button updateBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(10, updateBtn, deleteBtn);

            {
                // Update user action
                updateBtn.setOnAction(e -> {
                    try {
                        Utilisateur user = getTableView().getItems().get(getIndex());

                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UpdateUser.fxml"));
                        Parent root = loader.load();

                        UpdateUserController controller = loader.getController();
                        controller.setUser(user); // pass the selected user

                        Stage stage = (Stage) userTable.getScene().getWindow();
                        stage.setScene(new Scene(root));

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                // Delete user action
                deleteBtn.setOnAction(e -> {
                    Utilisateur user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void deleteUser(Utilisateur user) {
        String sql = "DELETE FROM user WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, user.getId());
            ps.executeUpdate();
            userList.remove(user);
            statusLabel.setText("User deleted successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed to delete user!");
        }
    }

    private void filterUsers() {
        String query = searchField.getText().toLowerCase();
        if (query.isEmpty()) {
            userTable.setItems(userList);
            return;
        }

        ObservableList<Utilisateur> filtered = FXCollections.observableArrayList();
        for (Utilisateur user : userList) {
            if (user.getNom().toLowerCase().contains(query) ||
                    user.getPrenom().toLowerCase().contains(query) ||
                    user.getEmail().toLowerCase().contains(query)) {
                filtered.add(user);
            }
        }

        userTable.setItems(filtered);
    }
}
