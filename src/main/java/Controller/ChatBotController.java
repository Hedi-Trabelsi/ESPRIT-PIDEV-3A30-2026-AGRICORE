package Controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import services.ChatBotService;

public class ChatBotController {

    private final ChatBotService chatBotService;
    private final StackPane parentContainer;

    private Button floatingButton;
    private VBox chatPanel;
    private VBox messagesBox;
    private ScrollPane scrollPane;
    private TextField inputField;
    private Button sendButton;
    private boolean chatVisible = false;
    private boolean isWaitingForResponse = false;

    public ChatBotController(StackPane parentContainer) {
        this.parentContainer = parentContainer;
        this.chatBotService = new ChatBotService();
        buildFloatingButton();
        buildChatPanel();

        // Position floating button bottom-right
        StackPane.setAlignment(floatingButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(floatingButton, new Insets(0, 20, 20, 0));

        // Position chat panel bottom-right
        StackPane.setAlignment(chatPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatPanel, new Insets(0, 20, 80, 0));

        // Initially hidden
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);

        // Add to parent
        parentContainer.getChildren().addAll(chatPanel, floatingButton);
    }

    /**
     * Re-adds the chatbot overlay nodes to the parent container.
     * Call this after replacing page content with setAll().
     */
    public void reattach() {
        if (!parentContainer.getChildren().contains(floatingButton)) {
            parentContainer.getChildren().addAll(chatPanel, floatingButton);
        }
    }

    private void buildFloatingButton() {
        floatingButton = new Button("\uD83D\uDCAC");
        floatingButton.setMinSize(50, 50);
        floatingButton.setMaxSize(50, 50);
        floatingButton.setShape(new Circle(25));
        floatingButton.setStyle(
                "-fx-background-color: #2e7d32;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 22px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);"
        );
        floatingButton.setOnAction(e -> toggleChat());

        floatingButton.setOnMouseEntered(e ->
                floatingButton.setStyle(
                        "-fx-background-color: #1b5e20;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 22px;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 5);"
                )
        );
        floatingButton.setOnMouseExited(e ->
                floatingButton.setStyle(
                        "-fx-background-color: #2e7d32;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 22px;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);"
                )
        );

        // Prevent click from going through to content behind
        floatingButton.setPickOnBounds(true);
    }

    private void buildChatPanel() {
        chatPanel = new VBox();
        chatPanel.setPrefSize(400, 500);
        chatPanel.setMaxSize(400, 500);
        chatPanel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 15;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 15, 0, 0, 5);"
        );

        // Header
        HBox header = buildHeader();

        // Messages area
        messagesBox = new VBox(10);
        messagesBox.setPadding(new Insets(15));
        messagesBox.setStyle("-fx-background-color: #f5f5f5;");

        scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: #f5f5f5; -fx-background: #f5f5f5;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Input area
        HBox inputArea = buildInputArea();

        chatPanel.getChildren().addAll(header, scrollPane, inputArea);

        // Add welcome message
        addBotMessage("Bonjour ! Je suis l'assistant AGRICOR. Comment puis-je vous aider ?");

        // Prevent clicks from going through
        chatPanel.setPickOnBounds(true);
    }

    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 15, 12, 15));
        header.setStyle(
                "-fx-background-color: #2e7d32;" +
                        "-fx-background-radius: 15 15 0 0;"
        );

        Label icon = new Label("\uD83E\uDD16");
        icon.setStyle("-fx-font-size: 20px;");

        Label title = new Label("Assistant AGRICOR");
        title.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 2 8;"
        );
        closeBtn.setOnAction(e -> toggleChat());
        closeBtn.setOnMouseEntered(e ->
                closeBtn.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.2);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 2 8;" +
                                "-fx-background-radius: 5;"
                )
        );
        closeBtn.setOnMouseExited(e ->
                closeBtn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 2 8;"
                )
        );

        header.getChildren().addAll(icon, title, spacer, closeBtn);
        return header;
    }

    private HBox buildInputArea() {
        HBox inputArea = new HBox(8);
        inputArea.setAlignment(Pos.CENTER);
        inputArea.setPadding(new Insets(10, 15, 10, 15));
        inputArea.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 0 0 15 15;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 1 0 0 0;"
        );

        inputField = new TextField();
        inputField.setPromptText("Tapez votre message...");
        inputField.setStyle(
                "-fx-background-color: #f5f5f5;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 8 15;" +
                        "-fx-font-size: 13px;"
        );
        HBox.setHgrow(inputField, Priority.ALWAYS);

        inputField.setOnAction(e -> sendMessage());

        sendButton = new Button("➤");
        sendButton.setMinSize(36, 36);
        sendButton.setMaxSize(36, 36);
        sendButton.setStyle(
                "-fx-background-color: #2e7d32;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 18;" +
                        "-fx-cursor: hand;"
        );
        sendButton.setOnAction(e -> sendMessage());

        inputArea.getChildren().addAll(inputField, sendButton);
        return inputArea;
    }

    private void toggleChat() {
        chatVisible = !chatVisible;
        chatPanel.setVisible(chatVisible);
        chatPanel.setManaged(chatVisible);

        if (chatVisible) {
            Platform.runLater(() -> {
                inputField.requestFocus();
                scrollToBottom();
            });
        }
    }

    private void sendMessage() {
        if (isWaitingForResponse) return;

        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        // Clear input and disable controls
        inputField.clear();
        setInputDisabled(true);

        // Add user message
        addUserMessage(text);

        // Show typing indicator
        VBox typingIndicator = createTypingIndicator();
        messagesBox.getChildren().add(typingIndicator);
        scrollToBottom();

        // Send to API
        isWaitingForResponse = true;

        chatBotService.sendMessage(text).thenAccept(response -> {
            Platform.runLater(() -> {
                // Remove typing indicator
                messagesBox.getChildren().remove(typingIndicator);

                // Add bot response
                addBotMessage(response);

                // Re-enable input
                setInputDisabled(false);
                isWaitingForResponse = false;
            });
        }).exceptionally(error -> {
            Platform.runLater(() -> {
                // Remove typing indicator
                messagesBox.getChildren().remove(typingIndicator);

                // Show error message
                addBotMessage("Désolé, une erreur s'est produite. Veuillez réessayer dans quelques instants.");

                // Re-enable input
                setInputDisabled(false);
                isWaitingForResponse = false;
            });
            return null;
        });
    }

    private void setInputDisabled(boolean disabled) {
        inputField.setDisable(disabled);
        sendButton.setDisable(disabled);

        if (disabled) {
            inputField.setPromptText("Attente de réponse...");
        } else {
            inputField.setPromptText("Tapez votre message...");
            inputField.requestFocus();
        }
    }

    private VBox createTypingIndicator() {
        Label typingLabel = new Label("Assistant AGRICOR écrit");
        typingLabel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-padding: 8 12;" +
                        "-fx-background-radius: 12 12 12 0;" +
                        "-fx-font-size: 12px;" +
                        "-fx-text-fill: #666;"
        );

        // Add animated dots
        HBox dots = new HBox(3);
        dots.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 3; i++) {
            Label dot = new Label("•");
            dot.setStyle(
                    "-fx-font-size: 16px;" +
                            "-fx-text-fill: #2e7d32;" +
                            "-fx-opacity: 0.7;"
            );
            dots.getChildren().add(dot);
        }

        HBox container = new HBox(5);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().addAll(typingLabel, dots);

        VBox wrapper = new VBox(container);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(0, 50, 0, 0));

        return wrapper;
    }

    private void addUserMessage(String text) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(280);
        msg.setStyle(
                "-fx-background-color: #2e7d32;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10 14;" +
                        "-fx-background-radius: 12 12 0 12;" +
                        "-fx-font-size: 13px;"
        );

        HBox box = new HBox(msg);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(0, 0, 0, 50));
        messagesBox.getChildren().add(box);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        // Add assistant label
        Label assistantLabel = new Label("Assistant AGRICOR");
        assistantLabel.setStyle(
                "-fx-font-size: 11px;" +
                        "-fx-text-fill: #666;" +
                        "-fx-padding: 0 0 2 5;"
        );

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(280);
        msg.setStyle(
                "-fx-background-color: white;" +
                        "-fx-text-fill: #333;" +
                        "-fx-padding: 10 14;" +
                        "-fx-background-radius: 12 12 12 0;" +
                        "-fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 2, 0, 0, 1);"
        );

        VBox messageContainer = new VBox(2);
        messageContainer.getChildren().addAll(assistantLabel, msg);

        HBox box = new HBox(messageContainer);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 50, 0, 0));

        messagesBox.getChildren().add(box);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            scrollPane.setVvalue(1.0);
        });
    }
}