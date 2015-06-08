package failchat.goodgame;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Message;
import failchat.core.Source;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GGMessage extends Message {

    @Override
    @JsonProperty("user_name")
    public void setAuthor(String author) {
        super.setAuthor(author);
    }

    @Override
    public Source getSource() {
        return Source.GOODGAME;
    }
}
