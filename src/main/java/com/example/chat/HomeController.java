package com.example.chat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HomeController implements Initializable {

    @FXML
    private VBox mainVbox;

    private boolean isRead = false;

    private void showUser(Image image, String userName, String text, String time, boolean isGuide) {
        AnchorPane mainAnchorpane = new AnchorPane();
        mainAnchorpane.setPrefHeight(80);
        mainAnchorpane.setStyle("-fx-background-color: white");

        Circle profilePicture = new Circle();
        profilePicture.setFill(new ImagePattern(image));
        profilePicture.setRadius(30);
        profilePicture.setLayoutX(60);
        profilePicture.setLayoutY(40);
        profilePicture.setSmooth(true);

        String designation = isGuide ? "(Guide)" : "(Traveller)";

        Label name = new Label(userName + "  " + designation);
        name.setLayoutX(110);
        name.setLayoutY(13);
        name.setStyle("-fx-font-family: 'Poppins'; -fx-font-size: 15px;-fx-font-weight: bold");

        Label message = new Label(text);
        message.setLayoutX(110);
        message.setLayoutY(45);
        message.setStyle("-fx-font-family: 'Poppins';-fx-font-size: 13px;");
        message.setMaxWidth(250);

        Label messageTime = new Label(time);
        messageTime.setLayoutY(45);
        messageTime.setLayoutX(370);
        messageTime.setStyle("-fx-font-family: 'Poppins';-fx-font-size: 13px; -fx-text-fill: #0088ff;");

        Label dot = new Label("·");
        dot.setLayoutY(5);
        dot.setLayoutX(550);

        updateDotColor(dot);

        mainAnchorpane.getChildren().add(dot);
        mainAnchorpane.setOnMouseClicked(event -> handler(event, userName));
        mainAnchorpane.getChildren().add(messageTime);
        mainAnchorpane.getChildren().add(message);
        mainAnchorpane.getChildren().add(name);
        mainAnchorpane.getChildren().add(profilePicture);
        mainVbox.getChildren().add(mainAnchorpane);
    }

    private void updateDotColor(Label dot) {
        if (isRead) {
            dot.setStyle("-fx-font-weight: bold;-fx-font-size: 50px; -fx-text-fill: black");
        } else {
            dot.setStyle("-fx-font-weight: bold;-fx-font-size: 50px; -fx-text-fill: #00b164");
        }
    }

    private void handler(javafx.scene.input.MouseEvent mouseEvent, String name) {
        isRead = true;
        AnchorPane sourcePane = (AnchorPane) mouseEvent.getSource();
        Label dot = (Label) sourcePane.getChildren().get(0);
        updateDotColor(dot);

        openUserDetailsScene(name);
    }

    private String currentTime() {
        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        return currentTime.format(formatter);
    }

    private void openUserDetailsScene(String userName) {
        try (Connection connection = getConnection()) {
            String selectLastMessageSQL = "SELECT id, message, sending_time FROM " + userName + " ORDER BY id DESC LIMIT 1";

            try (PreparedStatement preparedStatement1 = connection.prepareStatement(selectLastMessageSQL)) {
                ResultSet resultSet1 = preparedStatement1.executeQuery();

                if (resultSet1.next()) {
                    int lastID = resultSet1.getInt("id");
                    String lastMessage = resultSet1.getString("message");
                    String lastMessageTime = resultSet1.getString("sending_time");

                    FXMLLoader loader = new FXMLLoader(HomeController.class.getResource("Inbox.fxml"));
                    Parent root = loader.load();

                    InboxController userDetailsController = loader.getController();
                    userDetailsController.setUserName(userName);
                    userDetailsController.setLastMessage(lastMessage);
                    userDetailsController.setLastMessageTime(lastMessageTime);

                    Scene userDetailsScene = new Scene(root);

                    // Get the current stage
                    Stage currentStage = (Stage) mainVbox.getScene().getWindow();

                    // Set the new scene
                    currentStage.setScene(userDetailsScene);
                    currentStage.show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try (Connection connection = getConnection()) {
                String selectMessageSQL = "SELECT * FROM users" + " WHERE user_id > ? ORDER BY user_id";

                try (PreparedStatement preparedStatement = connection.prepareStatement(selectMessageSQL)) {
                    preparedStatement.setInt(1, lastMessageId);

                    ResultSet resultSet = preparedStatement.executeQuery();

                    while (resultSet.next()) {
                        int userID = resultSet.getInt("user_id");
                        String userName = resultSet.getString("username");

                        System.out.println(userName);

                        String selectLastMessageSQL = "SELECT id, message, sending_time FROM " + userName + " ORDER BY id DESC LIMIT 1";

                        String last_message = "";
                        String last_message_time = "";

                        try (PreparedStatement preparedStatement1 = connection.prepareStatement(selectLastMessageSQL)) {
                            ResultSet resultSet1 = preparedStatement1.executeQuery();

                            if (resultSet1.next()) {
                                last_message_time = resultSet1.getString("sending_time");
                                last_message = resultSet1.getString("message");
                            }
                        }

                        String finalLast_message = last_message;
                        String finalLast_message_time = last_message_time;
                        Platform.runLater(() -> {
                            showUser(image, userName, finalLast_message, finalLast_message_time, false);
                        });

                        // Update the lastMessageId to the latest ID
                        lastMessageId = userID;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/chatdb";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "";

    private int lastMessageId = 0;
    Image image = new Image(getClass().getResourceAsStream("/com/example/chat/user.png"));
}
