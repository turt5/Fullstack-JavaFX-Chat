package com.example.chat;

import io.github.palexdev.materialfx.controls.MFXScrollPane;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UserMessageController implements Initializable {
    public MFXScrollPane scrollPane;
    public AnchorPane mainAnchorpane;
    @FXML
    private MFXButton sendButton;
    @FXML
    private VBox messageFieldVbox;
    @FXML
    private Label userNameLabel;
    @FXML
    private MFXButton back;
    @FXML
    private MFXTextField textField;
    String[] userName = {""};
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/chatdb";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "";

    public void setUserName(String userName) {
        userNameLabel.setText(userName);
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    private void insertUser(String username) {
        try (Connection connection = getConnection()) {
            String checkUserSql = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement checkUserStatement = connection.prepareStatement(checkUserSql)) {
                checkUserStatement.setString(1, username);
                try (ResultSet resultSet = checkUserStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        String insertUserSql = "INSERT INTO users (username) VALUES (?)";
                        try (PreparedStatement insertUserStatement = connection.prepareStatement(insertUserSql)) {
                            insertUserStatement.setString(1, username);
                            insertUserStatement.executeUpdate();
                        }

                        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + username + " (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY," +
                                "from_ VARCHAR(255) NOT NULL," +
                                "to_ VARCHAR(255) NOT NULL," +
                                "message TEXT NOT NULL," +
                                "sending_time TEXT NOT NULL"+
                                ")";

                        try (PreparedStatement preparedStatement = connection.prepareStatement(createTableSQL)) {
                            preparedStatement.executeUpdate();
                            System.out.println("Table " + username + " created successfully.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private class SendMessageTask extends Task<Void> {
        private String message;
        private final boolean isIncoming;
        private String time;

        public SendMessageTask(String message, boolean isIncoming, String time) {
            this.message = message;
            this.isIncoming = isIncoming;
            this.time=time;
        }

        @Override
        protected Void call() throws Exception {
            message = message.replaceAll("~", "\n");
            String finalMessage = message.trim();

            try (Connection connection = getConnection()) {
                String tableName = userName[0];
                String fromUser = userName[0];
                String toUser = "admin";

                String insertMessageSql = "INSERT INTO " + tableName + " (from_, to_, message, sending_time) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertMessageStatement = connection.prepareStatement(insertMessageSql)) {
                    insertMessageStatement.setString(1, fromUser);
                    insertMessageStatement.setString(2, toUser);
                    insertMessageStatement.setString(3, finalMessage);
                    insertMessageStatement.setString(4, this.time);
                    insertMessageStatement.executeUpdate();
                }

//                Platform.runLater(()->{
//                    displayMessage(finalMessage, fromUser);
//                    scrollPane.setVvalue(1.0);
//                });
//                lastMessageId++;

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private String currentTime() {
        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        return currentTime.format(formatter);
    }

    private void handleEnterKeyPressed(MFXTextField textField) {
        String enteredText = textField.getText();
        enteredText += "~";
        textField.setText(enteredText);
        textField.positionCaret(enteredText.length());
    }

    private void displayMessage(String message, String fromUser) {
        stage=(Stage) mainAnchorpane.getScene().getWindow();
        stage.setTitle(userName[0]);
        message = message.replaceAll("~", "\n");
        String finalMessage = message.trim();

        HBox hBox = new HBox();
        hBox.setPadding(new Insets(10, 20, 0, 20));

        Text text = new Text(finalMessage);
//        text.setFill(Color.rgb(255, 255, 255));

        TextFlow textFlow = new TextFlow(text);
        String style;

        if ("admin".equals(fromUser)) {
            style = "-fx-background-color: #c7c7c7;" +
                    "-fx-text-fill: black;" +
                    "-fx-padding: 5 10 5 10px;" +
                    "-fx-font-size: 14px;" +
                    "-fx-background-radius: 20px;" +
                    "-fx-max-width: 400px";

        } else {
            style = "-fx-background-color: #0066ff;" +
                    "-fx-text-fill: white;" +
                    "-fx-padding: 5 10 5 10px;" +
                    "-fx-font-size: 14px;" +
                    "-fx-background-radius: 15px;" +
                    "-fx-max-width: 400px";
            text.setFill(Color.rgb(255, 255, 255));
            // Align to the right if the message is from the user
            hBox.setAlignment(fromUser.equals(userName[0]) ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        }

        textFlow.setStyle(style);
        textFlow.setPadding(new Insets(10, 10, 0, 10));

        hBox.getChildren().add(textFlow);

        messageFieldVbox.getChildren().add(hBox);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {



        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Username");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter your username:");

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            userName[0] = result.get();
            insertUser(userName[0]); // Insert or check existence of the user




            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(() -> {
                try (Connection connection = getConnection()) {
                    String tableName = userName[0];
                    String selectMessageSQL = "SELECT * FROM " + tableName + " WHERE id > ? ORDER BY id";

                    try (PreparedStatement preparedStatement = connection.prepareStatement(selectMessageSQL)) {
                        preparedStatement.setInt(1, lastMessageId);

                        ResultSet resultSet = preparedStatement.executeQuery();

                        while (resultSet.next()) {
                            int messageId = resultSet.getInt("id");
                            String fromUser = resultSet.getString("from_");
                            String message = resultSet.getString("message");
                            boolean isIncoming = !fromUser.equals(userName[0]);

                            Platform.runLater(()->{
                                displayMessage(message, fromUser);
//                                scrollPane.setVvalue(1.0);
                            });

                            // Update the lastMessageId to the latest ID
                            lastMessageId = messageId;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 1, TimeUnit.SECONDS);

//            }
        } else {
            Platform.exit(); // Terminate the program if the dialog is canceled
        }

        messageFieldVbox.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0);
        });

        back.setOnAction(event -> {
//            try {
//                FXMLLoader loader = new FXMLLoader(getClass().getResource("Home.fxml"));
//                Parent root = loader.load();
//                Scene previousScene = new Scene(root);
//                Stage currentStage = (Stage) back.getScene().getWindow();
//                currentStage.setScene(previousScene);
//                currentStage.show();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            Platform.exit();
        });

        textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleEnterKeyPressed(textField);
                event.consume(); // Consume the event to prevent it from being processed further
            }
        });

        sendButton.setOnAction(e -> {
            String text = textField.getText().trim();
            sendMessage(text, false,currentTime());
            textField.clear();
        });



    }

    private void sendMessage(String message, boolean isIncoming, String time) {
        SendMessageTask sendMessageTask = new SendMessageTask(message, !isIncoming, time);
        Thread thread = new Thread(sendMessageTask);
        thread.setDaemon(true);
        thread.start();
    }
    private int lastMessageId = 0;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
