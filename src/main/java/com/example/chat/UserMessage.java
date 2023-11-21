package com.example.chat;

public class UserMessage {
    private String userName;
    private String lastMessageTime;
    private String message;

    public UserMessage(String userName, String lastMessageTime, String message) {
        this.userName = userName;
        this.lastMessageTime = lastMessageTime;
        this.message = message;
    }

    public String getUserName() {
        return userName;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public String getMessage() {
        return message;
    }
}
