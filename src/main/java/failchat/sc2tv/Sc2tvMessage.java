package failchat.sc2tv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Message;
import failchat.core.Source;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Sc2tvMessage extends Message {

    Sc2tvMessage() {
        source = Source.SC2TV;
    }

    @Override
    @JsonProperty("name")
    public void setAuthor(String author) {
        super.setAuthor(author);
    }

    @Override
    @JsonProperty("message")
    public void setText(String text) {
        super.setText(text);
    }

    @Override
    @JsonProperty("date")
    public void setTimestamp(Date timestamp) {
        super.setTimestamp(timestamp);
    }
}
