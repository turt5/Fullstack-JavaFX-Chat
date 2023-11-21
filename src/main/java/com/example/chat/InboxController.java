package com.example.chat;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InboxController implements Initializable {
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

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/chatdb";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "";

    public void setUserName(String userName) {
        userNameLabel.setText(userName);
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }


    private String generateUniqueMsgId() {
        return UUID.randomUUID().toString();
    }


    private void handleEnterKeyPressed(TextField textField) {
        String enteredText = textField.getText();
        enteredText += "~";
        textField.setText(enteredText);
        textField.positionCaret(enteredText.length());
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
                String tableName = userNameLabel.getText().trim();
                String fromUser = "admin";
                String toUser = userNameLabel.getText().trim();

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

    private void displayMessage(String message, String fromUser) {
        message = message.replaceAll("~", "\n");
        String finalMessage = message.trim();

        HBox hBox = new HBox();
        hBox.setPadding(new Insets(10, 20, 0, 20));

        Text text = new Text(finalMessage);
//        text.setFill(Color.rgb(255, 255, 255));

        TextFlow textFlow = new TextFlow(text);
        String style;

        System.out.println("Label: "+userNameLabel.getText().trim());
        System.out.println("user: "+fromUser);

        if (userNameLabel.getText().trim().equals(fromUser)) {
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
            hBox.setAlignment(fromUser.equals("admin") ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        }

        textFlow.setStyle(style);
        textFlow.setPadding(new Insets(10, 10, 0, 10));

        hBox.getChildren().add(textFlow);

        messageFieldVbox.getChildren().add(hBox);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try (Connection connection = getConnection()) {
                String tableName = userNameLabel.getText().trim();
//                System.out.println(tableName);
                String selectMessageSQL = "SELECT * FROM " + tableName + " WHERE id > ? ORDER BY id";

                try (PreparedStatement preparedStatement = connection.prepareStatement(selectMessageSQL)) {
                    preparedStatement.setInt(1, lastMessageId);

                    ResultSet resultSet = preparedStatement.executeQuery();

                    while (resultSet.next()) {
                        int messageId = resultSet.getInt("id");
                        String fromUser = resultSet.getString("from_");
                        String message = resultSet.getString("message");
                        boolean isIncoming = !fromUser.equals(userName[0]);

                        Platform.runLater(() -> {
                            displayMessage(message, fromUser);
                            // scrollPane.setVvalue(1.0);
                        });

                        // Update the lastMessageId to the latest ID
                        lastMessageId = messageId;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);



        back.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("Home.fxml"));
                Parent root = loader.load();
                Scene previousScene = new Scene(root);
                Stage currentStage = (Stage) back.getScene().getWindow();
                currentStage.setScene(previousScene);
                currentStage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleEnterKeyPressed(textField);
                event.consume(); // Consume the event to prevent it from being processed further
            }
        });


        sendButton.setOnAction(e -> {
            String text = textField.getText().trim();
//            insertMessage(text);
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

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    private String lastMessage;
    private String lastMessageTime;
    String[] userName = {""};
    int lastMessageId=0;
}
