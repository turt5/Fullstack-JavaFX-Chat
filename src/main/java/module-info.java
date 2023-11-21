module com.example.chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires MaterialFX;


    opens com.example.chat to javafx.fxml;
    exports com.example.chat;
}