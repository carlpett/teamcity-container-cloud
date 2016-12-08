package se.capeit.dev.containercloud.cloud.providers;

import java.util.ArrayList;
import java.util.List;

public class TestConnectionResult {
    private boolean ok;
    private final List<Message> messages;

    public TestConnectionResult() {
        this.ok = false;
        this.messages = new ArrayList<>();
    }

    public void setOk(boolean isOk) {
        ok = isOk;
    }

    public boolean isOk() {
        return ok;
    }

    public void addMessage(String msg, Message.MessageLevel level) {
        messages.add(new Message(msg, level));
    }

    public List<Message> getMessages() {
        return messages;
    }

    public static class Message {
        private final String message;
        private final MessageLevel level;

        Message(String message, MessageLevel level) {
            this.message = message;
            this.level = level;
        }

        public String getMessage() {
            return message;
        }

        public MessageLevel getLevel() {
            return level;
        }

        public enum MessageLevel { INFO, WARNING, ERROR }
    }
}

