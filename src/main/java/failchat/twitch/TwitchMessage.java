package failchat.twitch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import failchat.core.Message;
import failchat.core.Source;
import org.apache.commons.lang.StringUtils;
import org.pircbotx.hooks.events.MessageEvent;

import java.util.Date;

/**
 * Сообщение из irc-чата твитча. Может быть как сообщением от пользователя, так и мета-сообщением от jtv
 */
public class TwitchMessage extends Message {
    private static int[] globalEmoteSet = {0};

    private MetaProperties properties;

    TwitchMessage() {} //for jackson (exception related with JsonInclude.Include.NON_DEFAULT)

    TwitchMessage(MessageEvent event) {
        this.text = event.getMessage();
        this.timestamp = new Date();
        this.source = Source.TWITCH;

        String displayedName = event.getV3Tags().get("display-name"); //could return null (e.g. from twitchnotify)
        //если пользователь не менял ник, то в v3tags пусто, ник capitalized
        if (displayedName == null || displayedName.equals("")) {
            this.author = StringUtils.capitalize(event.getUserHostmask().getNick());
        }
        else {
            this.author = displayedName;
        }

        // TODO: emote sets and other properties
    }

    TwitchMessage(String author, String text) {
        this.author = author;
        this.text = text;
        this.timestamp = new Date();
        this.source = Source.TWITCH;
    }

    @JsonIgnore
    public int[] getEmoteSets() {
        return properties != null && properties.getEmoteSets() != null ? properties.getEmoteSets() : globalEmoteSet;
    }

    public void setProperties(MetaProperties properties) {
        this.properties = properties;
    }
}
