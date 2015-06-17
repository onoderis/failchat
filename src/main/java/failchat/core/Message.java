package failchat.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Message {
    private static final int MAS_INIT_SIZE = 3;
    private static final int MAS_INCR_SIZE = 10;

    protected String author;
    protected String text;
    protected Date timestamp;
    protected Source source;
    protected List<SmileInMessage> smiles;
    protected List<Url> links;

    private static String format(int objectNumber) {
        return "{!" + objectNumber + "}";
    }

    private int objectsCount = 0; //smiles and links count

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

    public List<Url> getLinks() {
        return links;
    }

    /**
     * @return formatted object number
     */
    public String addSmile (Smile smile) {
        objectsCount++;
        if (smiles == null) {
            smiles = new ArrayList<>();
        }
        smiles.add(new SmileInMessage(smile, objectsCount));
        return format(objectsCount);
    }

    public String addLink(Url url) {
        objectsCount++;
        url.setObjectNumber(objectsCount);
        if (links == null) {
            links = new ArrayList<>();
        }
        links.add(url);
        return format(objectsCount);
    }
}
