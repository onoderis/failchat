package failchat.goodgame;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Message;
import failchat.core.Source;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GGMessage extends Message {
    protected boolean premiumUser;

    GGMessage() {
        source = Source.GOODGAME;
    }

    @Override
    @JsonProperty("user_name")
    public void setAuthor(String author) {
        super.setAuthor(author);
    }

    @JsonIgnore
    public boolean isPremiumUser() {
        return premiumUser;
    }

    @JsonProperty("premium")
    public void setPremiumUser(boolean premiumUser) {
        this.premiumUser = premiumUser;
    }
}
