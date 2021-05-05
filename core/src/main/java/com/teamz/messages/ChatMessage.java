package com.teamz.messages;

public class ChatMessage {
    private String sender;
    private String channel;
    private String message;
    private String datetime;

    public ChatMessage() {}

    public ChatMessage(String sender, String message, String channel, String datetime) {
        this.sender = sender;
        this.message = message;
        this.channel = channel;
        this.datetime = datetime;
    }

	public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String toString() {
        if (message.equals("has joined the channel")) {
            return sender + " " + message;
        }
        if (message.equals("has left the channel")) {
            return sender + " " + message;
        }
        if (message.contains("is now " + sender)) {
            return message;
        }
        return "<" + sender + "> " + message;
    }
}
