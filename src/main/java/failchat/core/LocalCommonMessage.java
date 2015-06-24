package failchat.core;

public class LocalCommonMessage {
    private String type;
    private Object content;


    LocalCommonMessage(String type, Object content) {
        this.type = type;
        this.content = content;
    }

    public Object getContent() {
        return content;
    }

    public String getType() {
        return type;
    }
}
