package com.mittimitra;

public class ChatMessage {
    public enum Type {
        USER,
        BOT,
        LOADING // For the "Bot is typing..." message
    }

    private final String message;
    private final Type type;

    public ChatMessage(String message, Type type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public Type getType() {
        return type;
    }
}