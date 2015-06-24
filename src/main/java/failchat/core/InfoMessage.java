package failchat.core;

import java.util.Date;

public class InfoMessage extends Message {

    public InfoMessage() {} //for jackson

    public InfoMessage(Source source, String text) {
        this.source = source;
        this.text = text;
        this.timestamp = new Date();
    }
}
