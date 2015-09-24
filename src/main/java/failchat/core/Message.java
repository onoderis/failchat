package failchat.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    public static final AtomicInteger lastId = new AtomicInteger(Configurator.config.getInt("lastId"));

    protected int id;
    protected Source source;
    protected String author;
    protected String text;
    protected Date timestamp;
    protected List<SmileInMessage> smiles;
    protected List<Link> links;
    protected List<Image> images;
    protected boolean highlighted = false;

    private int objectsCount = 0; //smiles and links count

    public Message() {
        id = lastId.incrementAndGet();
    }

    private static String format(int objectNumber) {
        return "{!" + objectNumber + "}";
    }

    @JsonIgnore(true)
    public Source getSource() {
        return source;
    }

    @JsonProperty("source")
    public String getLowerCasedSource() {
        return getSource() != null ? source.getLowerCased() : null;
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

    @JsonProperty("smiles")
    public List<SmileInMessage> getSmiles() {
        return smiles;
    }

    public List<Link> getLinks() {
        return links;
    }

    public List<Image> getImages() {
        return images;
    }

    @JsonInclude (JsonInclude.Include.NON_DEFAULT)
    public boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    public int getId() {
        return id;
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

    /**
     * @return formatted object number
     */
    public String addLink(Link link) {
        objectsCount++;
        link.setObjectNumber(objectsCount);
        if (links == null) {
            links = new ArrayList<>();
        }
        links.add(link);
        return format(objectsCount);
    }

    /**
     * @return formatted object number
     */
    public String addImage(Image image) {
        objectsCount++;
        image.setObjectNumber(objectsCount);
        if (images == null) {
            images = new ArrayList<>();
        }
        images.add(image);
        return format(objectsCount);
    }
}
