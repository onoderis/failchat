package failchat.core;

public class LocalCommonMessage {
    private String type;
    private Message message;


    LocalCommonMessage(Message m) {
        message = m;
        type = "message";
    }

    public Message getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }
}
