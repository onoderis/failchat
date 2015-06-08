package failchat.twitch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import failchat.core.Message;
import failchat.core.Source;

import java.util.Date;

/**
 * Сообщение из irc-чата твитча. Может быть как сообщением от пользователя, так и мета-сообщением от jtv
 */
public class TwitchMessage extends Message {
    private static int[] globalEmoteSet = {0};

    private MetaProperties properties;

    TwitchMessage(String author, String text) {
        this.author = author;
        this.text = text;
        this.timestamp = new Date();
    }

    @JsonIgnore
    public int[] getEmoteSets() {
        return properties != null && properties.getEmoteSets() != null ? properties.getEmoteSets() : globalEmoteSet;
    }

    @Override
    public Source getSource() {
        return Source.TWITCH;
    }

    public void setProperties(MetaProperties properties) {
        this.properties = properties;
    }
}
