package failchat.twitch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import failchat.core.Message;
import failchat.core.Source;
import org.apache.commons.lang.StringUtils;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;

import java.util.Date;

public class TwitchMessage extends Message {
    protected String usedSmiles;

    TwitchMessage() {} //for jackson (exception related with JsonInclude.Include.NON_DEFAULT)

    TwitchMessage(MessageEvent event) {
        this.text = event.getMessage();
        this.usedSmiles = event.getV3Tags().get("emotes");
        this.timestamp = new Date(event.getTimestamp());
        this.source = Source.TWITCH;

        String displayedName = event.getV3Tags().get("display-name"); //could return null (e.g. from twitchnotify)
        //если пользователь не менял ник, то в v3tags пусто, ник capitalized
        if (displayedName == null || displayedName.equals("")) {
            this.author = StringUtils.capitalize(event.getUserHostmask().getNick());
        }
        else {
            this.author = displayedName;
        }
    }

    TwitchMessage(ActionEvent event) {
        this.text = event.getMessage();
        this.timestamp = new Date(event.getTimestamp());
        this.source = Source.TWITCH;
        this.author = event.getUserHostmask().getNick();
    }

    @JsonIgnore
    public String getUsedSmiles() {
        return usedSmiles;
    }
}
