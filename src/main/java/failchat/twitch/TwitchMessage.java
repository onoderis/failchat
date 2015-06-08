package failchat.twitch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import failchat.core.Message;
import failchat.core.Source;

import java.util.Date;

/**
 * Сообщение из irc-чата твитча. Может быть как сообщением от пользователя, так и мета-сообщением от jtv
 */
public class TwitchMessage extends Message {
    private int[] emoteSets;
    private static int[] globalEmoteSet = {0};
    private boolean meta;

    TwitchMessage(String author, String text) {
        this.author = author;
        this.text = text;
        this.timestamp = new Date();
    }

    @JsonIgnore
    public int[] getEmoteSets() {
        if (emoteSets == null) {
            return globalEmoteSet;
        }
        return emoteSets;
    }

    public void setEmoteSets(int[] emoteSets) {
        this.emoteSets = emoteSets;
    }

    @JsonIgnore
    public boolean isMeta() {
        return meta;
    }

    public void setMeta(boolean meta) {
        this.meta = meta;
    }

    @Override
    public Source getSource() {
        return Source.TWITCH;
    }
}
