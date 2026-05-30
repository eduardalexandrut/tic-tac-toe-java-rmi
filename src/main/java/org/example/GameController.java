package org.example;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static javafx.application.Application.launch;

public class GameController extends Application {
    private GameEngine server;
    private GameClientListener listener;
    private Stage primaryStage;

    // Game State Variables
    private String username;
    private String matchName;

    // UI Elements for the Board
    private final Button[][] buttons = new Button[3][3];
    private Label boardStatusLabel;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Connect to the RMI server immediately when the app boots up
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            this.server = (GameEngine) registry.lookup("GameEngine");
            this.listener = new GameClientListenerImpl(this);
        } catch (Exception e) {
            showErrorLayout("Failed to connect to RMI Server. Make sure it's running!\n" + e.getMessage());
            return;
        }

        // Show the initial Login/Lobby View
        showLobbyScreen();
    }

    /**
     * SCREEN 1: The Lobby Screen where users enter their name and Create/Join a match.
     */
    private void showLobbyScreen() {
        primaryStage.setTitle("Tic-Tac-Toe - Login");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #f4f4f4;");

        Label titleLabel = new Label("Distributed Tic-Tac-Toe");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your Username");
        usernameField.setMaxWidth(250);

        TextField matchField = new TextField();
        matchField.setPromptText("Enter Match Name");
        matchField.setMaxWidth(250);

        Label feedbackLabel = new Label("");
        feedbackLabel.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");

        Button actionButton = new Button("Connect & Join Match");
        actionButton.setMinWidth(250);
        actionButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        actionButton.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String match = matchField.getText().trim();

            if (user.isEmpty() || match.isEmpty()) {
                feedbackLabel.setText("Both fields are required!");
                return;
            }

            try {
                this.username = user;
                this.matchName = match;

                // Attempt to register with the server
                Message msg = new Message(matchName, username);
                server.joinGame(msg, this.listener);

                // Transition to the Game Board Screen if successful
                showBoardScreen();

            } catch (Exception ex) {
                feedbackLabel.setText(ex.getMessage());
            }
        });

        layout.getChildren().addAll(titleLabel, usernameField, matchField, actionButton, feedbackLabel);

        Scene scene = new Scene(layout, 350, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * SCREEN 2: The actual 3x3 game board screen.
     */
    private void showBoardScreen() {
        primaryStage.setTitle("Match: " + matchName + " | Player: " + username);

        BorderPane root = new BorderPane();
        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        boardStatusLabel = new Label("Joined match: " + matchName + ". Waiting for opponent...");
        boardStatusLabel.setStyle("-fx-font-size: 15px; -fx-padding: 10px; -fx-font-weight: bold;");
        root.setBottom(boardStatusLabel);
        BorderPane.setAlignment(boardStatusLabel, Pos.CENTER);

        // Generate the board elements
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                Button btn = new Button(" ");
                btn.setMinSize(100, 100);
                btn.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

                final int finalX = x;
                final int finalY = y;

                btn.setOnAction(e -> handleGridSelection(finalX, finalY));

                buttons[y][x] = btn;
                gridPane.add(btn, x, y);
            }
        }
        root.setCenter(gridPane);

        Scene scene = new Scene(root, 400, 450);
        primaryStage.setScene(scene);
    }

    private void handleGridSelection(int x, int y) {
        try {
            boardStatusLabel.setText("Processing move...");
            server.makeMove(matchName, username, x, y);
        } catch (Exception e) {
            boardStatusLabel.setText("Error: " + e.getMessage());
        }
    }

    /**
     * Callbacks invoked via Platform.runLater from the RMI Listener thread
     */
    public void updateBoardState(String player, String symbol, int x, int y) {
        buttons[y][x].setText(symbol);
        buttons[y][x].setDisable(true);
        boardStatusLabel.setText("Last move: " + player + " (" + symbol + "). Your turn if valid!");
    }

    public void showGameOverState(String finalMessage) {
        boardStatusLabel.setText(finalMessage);
        for (int y = 0; y < 3; rTearDown(y)) {
            for (int x = 0; x < 3; x++) {
                buttons[y][x].setDisable(true);
            }
        }
    }

    private int rTearDown(int y) { return ++y; }

    public void updateStatus(String text) {
        boardStatusLabel.setText(text);
    }

    private void showErrorLayout(String errorMsg) {
        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        Label errLabel = new Label(errorMsg);
        errLabel.setStyle("-fx-text-fill: red; -fx-alignment: center; -fx-text-alignment: center;");
        layout.getChildren().add(errLabel);
        Scene scene = new Scene(layout, 400, 200);
        primaryStage.setTitle("Error Connection");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
