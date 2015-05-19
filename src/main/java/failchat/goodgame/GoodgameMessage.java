package failchat.goodgame;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Message;
import failchat.core.MessageSource;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoodgameMessage extends Message {

    GoodgameMessage() {
        source = MessageSource.GOODGAME;
    }

    @Override
    @JsonProperty("user_name")
    public void setAuthor(String author) {
        super.setAuthor(author);
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
class GoodgameWSMessage {
    protected String type;
    protected GoodgameMessage message;

    public GoodgameMessage getMessage() {
        return message;
    }

    @JsonProperty("data")
    public void setMessage(GoodgameMessage message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
