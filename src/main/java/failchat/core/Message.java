package failchat.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Message {

    protected String author;
    protected String text;
    protected Date timestamp;
    protected Source source;
    protected List<SmileInMessage> smiles;

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    @JsonProperty("author")
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @JsonProperty("text")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<SmileInMessage> getSmiles() {
        return smiles;
    }

    // отдельный метод для того, чтобы smiles не шли в json если их нет в сообщении
    @JsonIgnore
    public List<SmileInMessage> getSml() {
        if (smiles == null) {
            smiles = new ArrayList<>();
        }
        return smiles;
    }

    public void setSmiles(List<SmileInMessage> smiles) {
        this.smiles = smiles;
    }
}
