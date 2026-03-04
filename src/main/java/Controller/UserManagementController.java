package Controller;

import Model.Utilisateur;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import services.UserService;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class UserManagementController {

    @FXML private FlowPane usersGrid;
    @FXML private Pagination pagination;
    @FXML private Label showingLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private ListView<String> activityList;
    @FXML private Label totalUsersLabel, totalAdminsLabel, totalFarmersLabel;

    private Utilisateur loggedInUser;
    private HomeController homeController;
    private UserService userService;
    private ObservableList<Utilisateur> masterData = FXCollections.observableArrayList();
    private FilteredList<Utilisateur> filteredData;
    private static final int CARDS_PER_PAGE = 6;

    @FXML
    public void initialize() {
        try {
            userService = new UserService();

            // Initialize filteredData
            filteredData = new FilteredList<>(masterData, p -> true);

            setupSearchAndFilter();
            setupPagination();

            // Set FlowPane properties
            if (usersGrid != null) {
                usersGrid.setAlignment(Pos.TOP_LEFT);
                usersGrid.setHgap(20);
                usersGrid.setVgap(20);
                usersGrid.setPadding(new Insets(10, 0, 0, 0));
            }

            // FXML already has filter items, just set default value
            if (filterCombo != null) {
                filterCombo.setValue("Tous les roles");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLoggedInUser(Utilisateur user) {
        this.loggedInUser = user;
        // Load users immediately when user is set
        loadAllUsers();
    }

    public void setHomeController(HomeController controller) {
        this.homeController = controller;
    }

    private void setupSearchAndFilter() {
        if (searchField != null && filterCombo != null && filteredData != null) {
            searchField.textProperty().addListener((obs, old, newValue) -> {
                applyFilters();
            });

            filterCombo.valueProperty().addListener((obs, old, newValue) -> {
                applyFilters();
            });
        }
    }

    private void applyFilters() {
        if (filteredData == null) return;

        String searchText = searchField.getText();
        String roleFilter = filterCombo.getValue();

        filteredData.setPredicate(user -> {
            // Search filter
            if (searchText != null && !searchText.isEmpty()) {
                String search = searchText.toLowerCase();
                if (!user.getNom().toLowerCase().contains(search) &&
                        !user.getPrenom().toLowerCase().contains(search) &&
                        !user.getEmail().toLowerCase().contains(search) &&
                        !String.valueOf(user.getPhone()).contains(search)) {
                    return false;
                }
            }

            // Role filter
            if (roleFilter != null && !roleFilter.equals("Tous les roles")) {
                if (!getRoleText(user.getRole()).equals(roleFilter)) {
                    return false;
                }
            }

            return true;
        });

        updatePagination();
    }

    private void setupPagination() {
        if (pagination != null) {
            // Return a tiny placeholder so the Pagination does NOT steal usersGrid
            pagination.setPageFactory(pageIndex -> {
                updateGrid(pageIndex);
                Region placeholder = new Region();
                placeholder.setMaxHeight(0);
                placeholder.setPrefHeight(0);
                return placeholder;
            });
        }
    }

    private void updateGrid(int pageIndex) {
        if (usersGrid != null) {
            usersGrid.getChildren().clear();
        }

        if (filteredData == null || filteredData.isEmpty()) {
            if (showingLabel != null) {
                showingLabel.setText("Affichage de 0 utilisateurs");
            }
            return;
        }

        int fromIndex = pageIndex * CARDS_PER_PAGE;
        int toIndex = Math.min(fromIndex + CARDS_PER_PAGE, filteredData.size());

        if (fromIndex < filteredData.size() && usersGrid != null) {
            List<Utilisateur> pageUsers = filteredData.subList(fromIndex, toIndex);
            for (Utilisateur user : pageUsers) {
                VBox card = createUserCard(user);
                usersGrid.getChildren().add(card);
            }

            usersGrid.setAlignment(Pos.TOP_LEFT);
            usersGrid.requestLayout();
        }

        if (showingLabel != null) {
            showingLabel.setText(String.format("Affichage %d-%d sur %d utilisateurs",
                    fromIndex + 1, toIndex, filteredData.size()));
        }
    }

    private VBox createUserCard(Utilisateur user) {
        VBox card = new VBox(8);
        card.getStyleClass().add("user-card");
        card.setPrefWidth(280);
        card.setPrefHeight(320);
        card.setMaxWidth(280);
        card.setMaxHeight(320);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(12, 12, 12, 12));

        // Photo
        ImageView photoView = new ImageView();
        photoView.setFitWidth(90);
        photoView.setFitHeight(90);
        photoView.setPreserveRatio(false);
        photoView.getStyleClass().add("card-photo");

        if (user.getImage() != null && user.getImage().length > 0) {
            try {
                Image img = new Image(new ByteArrayInputStream(user.getImage()));
                photoView.setImage(img);
            } catch (Exception e) {
                setDefaultAvatar(photoView, user.getNom());
            }
        } else {
            setDefaultAvatar(photoView, user.getNom());
        }

        // User name
        Label nameLabel = new Label(user.getNom() + " " + user.getPrenom());
        nameLabel.getStyleClass().add("card-name");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMaxWidth(250);

        // Role badge
        Label roleLabel = new Label(getRoleText(user.getRole()));
        roleLabel.getStyleClass().add("card-role");
        roleLabel.setAlignment(Pos.CENTER);

        // Email
        Label emailLabel = new Label(user.getEmail());
        emailLabel.getStyleClass().add("card-email");
        emailLabel.setWrapText(true);
        emailLabel.setAlignment(Pos.CENTER);
        emailLabel.setMaxWidth(250);

        // Separator
        Separator separator = new Separator();
        separator.setPrefWidth(250);
        separator.setPadding(new Insets(3, 0, 3, 0));

        // Info grid
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(8);
        infoGrid.setVgap(3);
        infoGrid.setAlignment(Pos.CENTER);

        // Gender
        Label genderIcon = new Label(user.getGenre() != null && user.getGenre().equals("Male") ? "👨" : "👩");
        genderIcon.getStyleClass().add("card-icon");

        Label genderLabel = new Label(user.getGenre() != null ? user.getGenre() : "N/A");
        genderLabel.getStyleClass().add("card-info-text");

        // Phone
        Label phoneIcon = new Label("📞");
        phoneIcon.getStyleClass().add("card-icon");

        Label phoneLabel = new Label(String.valueOf(user.getPhone()));
        phoneLabel.getStyleClass().add("card-info-text");

        infoGrid.add(genderIcon, 0, 0);
        infoGrid.add(genderLabel, 1, 0);
        infoGrid.add(phoneIcon, 2, 0);
        infoGrid.add(phoneLabel, 3, 0);

        // Birth date
        Label birthIcon = new Label("🎂");
        birthIcon.getStyleClass().add("card-icon");

        String birthDate = user.getDateNaissance() != null ?
                user.getDateNaissance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A";
        Label birthLabel = new Label(birthDate);
        birthLabel.getStyleClass().add("card-info-text");

        infoGrid.add(birthIcon, 0, 1);
        infoGrid.add(birthLabel, 1, 1, 3, 1);

        // Address
        Label addressLabel = new Label("📍 " + (user.getAdresse() != null ? user.getAdresse() : "No address"));
        addressLabel.getStyleClass().add("card-address");
        addressLabel.setWrapText(true);
        addressLabel.setMaxWidth(250);
        addressLabel.setAlignment(Pos.CENTER);
        addressLabel.setPadding(new Insets(3, 0, 3, 0));

        // Action buttons
        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(3, 0, 0, 0));

        Button updateBtn = new Button("Modifier");
        updateBtn.getStyleClass().add("update-button");
        updateBtn.setPrefWidth(75);
        updateBtn.setOnAction(e -> handleUpdateUser(user));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setPrefWidth(75);
        deleteBtn.setOnAction(e -> handleDeleteUser(user));

        actionBox.getChildren().addAll(updateBtn, deleteBtn);

        card.getChildren().addAll(
                photoView,
                nameLabel,
                roleLabel,
                emailLabel,
                separator,
                infoGrid,
                addressLabel,
                actionBox
        );

        return card;
    }

    private void setDefaultAvatar(ImageView imageView, String name) {
        imageView.setImage(null);
        imageView.setStyle("-fx-background-color: #1b5e20; -fx-background-radius: 5;");
    }

    private String getRoleText(int roleValue) {
        return switch (roleValue) {
            case 0 -> "Admin";
            case 1 -> "Agriculteur";
            case 2 -> "Technicien";
            case 3 -> "Fournisseur";
            default -> "Unknown";
        };
    }

    private void updatePagination() {
        if (filteredData == null) return;
        int totalPages = (filteredData.size() + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE;
        if (pagination != null) {
            pagination.setPageCount(Math.max(1, totalPages));
            pagination.setCurrentPageIndex(0);
        }
        // Always refresh the grid since setCurrentPageIndex(0) won't
        // re-trigger the page factory if it's already at 0
        updateGrid(0);
    }

    private void loadAllUsers() {
        try {
            List<Utilisateur> users = userService.read();
            System.out.println("UserManagement: Loading " + users.size() + " users");

            Platform.runLater(() -> {
                // Update the master list; filteredData wraps it and auto-updates
                masterData.setAll(users);

                // Reset filters
                if (searchField != null) searchField.clear();
                if (filterCombo != null) filterCombo.setValue("Tous les roles");

                // Reset predicate to show all users
                if (filteredData != null) {
                    filteredData.setPredicate(p -> true);
                }

                updatePagination();

                // Load statistics
                updateStatistics(users);
                loadRecentActivity();
            });
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStatistics(List<Utilisateur> users) {
        if (totalUsersLabel != null) {
            totalUsersLabel.setText(String.valueOf(users.size()));
        }
        if (totalAdminsLabel != null) {
            long admins = users.stream().filter(u -> u.getRole() == 0).count();
            totalAdminsLabel.setText(String.valueOf(admins));
        }
        if (totalFarmersLabel != null) {
            long farmers = users.stream().filter(u -> u.getRole() == 1).count();
            totalFarmersLabel.setText(String.valueOf(farmers));
        }
    }

    private void loadRecentActivity() {
        if (activityList != null) {
            activityList.getItems().setAll(
                    "Systeme demarre a " + LocalDate.now(),
                    "Module de gestion des utilisateurs charge",
                    "Connexion a la base de donnees etablie"
            );
        }
    }

    private void handleUpdateUser(Utilisateur user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditUser.fxml"));
            Parent root = loader.load();

            EditUserController controller = loader.getController();
            controller.setUser(user);
            controller.setHomeController(homeController);
            controller.setOnUserUpdated(() -> {
                Platform.runLater(() -> {
                    loadAllUsers();
                    showAlert("Succes", "Utilisateur modifie avec succes !", Alert.AlertType.INFORMATION);
                });
            });

            Stage stage = new Stage();
            stage.setTitle("Modifier l'utilisateur");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la fenetre de modification : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleDeleteUser(Utilisateur user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer l'utilisateur");
        confirm.setContentText("Etes-vous sur de vouloir supprimer " + user.getNom() + " " + user.getPrenom() + " ?");

        if (usersGrid != null && usersGrid.getScene() != null) {
            confirm.initOwner(usersGrid.getScene().getWindow());
        }

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.delete(user.getId());
                Platform.runLater(() -> {
                    loadAllUsers();
                    showAlert("Succes", "Utilisateur supprime avec succes !", Alert.AlertType.INFORMATION);
                });
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur", "Echec de la suppression : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (usersGrid != null && usersGrid.getScene() != null) {
            alert.initOwner(usersGrid.getScene().getWindow());
        }
        alert.showAndWait();
    }
}