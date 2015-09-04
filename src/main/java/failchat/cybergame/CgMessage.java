package failchat.cybergame;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Message;
import failchat.core.Source;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CgMessage extends Message {
    CgMessage() {
        source = Source.CYBERGAME;
    }

    @Override
    @JsonProperty (value = "when")
    public void setTimestamp(Date timestamp) {
        super.setTimestamp(timestamp);
    }

    @Override
    @JsonProperty (value = "timestamp")
    public Date getTimestamp() {
        return super.getTimestamp();
    }

    @Override
    @JsonProperty (value = "from")
    public void setAuthor(String author) {
        super.setAuthor(author);
    }
}
