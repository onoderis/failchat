package failchat.twitch;

import failchat.core.Message;
import failchat.core.Source;

import java.util.Date;

public class TwitchMessage extends Message {
    private int[] emoteSets;
    private boolean meta;

    TwitchMessage(String author, String text) {
        this.author = author;
        this.text = text;
        this.timestamp = new Date();
        this.source = Source.TWITCH;
    }

    //constructor for meta messages
    TwitchMessage(String text) {
        this.text = text;
    }

    public int[] getEmoteSets() {
        return emoteSets;
    }

    public void setEmoteSets(int[] emoteSets) {
        this.emoteSets = emoteSets;
    }

    public boolean isMeta() {
        return meta;
    }

    public void setMeta(boolean meta) {
        this.meta = meta;
    }
}
