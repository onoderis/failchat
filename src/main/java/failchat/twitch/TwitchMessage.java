package failchat.twitch;

import failchat.core.Message;
import failchat.core.Source;

import java.util.Date;

public class TwitchMessage extends Message {

    TwitchMessage(String author, String text) {
        this.author = author;
        this.text = text;
        this.timestamp = new Date();
        this.source = Source.TWITCH;
    }
}
