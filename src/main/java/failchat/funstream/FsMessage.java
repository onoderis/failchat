package failchat.funstream;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Source;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FsMessage extends failchat.core.Message {
    protected int fsId;
    protected User from; //for SourceFilter and HighlightHandler
    protected User to; //for HighlightHandler
    protected String type;

    FsMessage() {
        this.source = Source.SC2TV;
    }

    @JsonIgnore
    public User getTo() {
        return to;
    }

    @JsonProperty(value = "to")
    public void setTo(User to) {
        this.to = to;
    }

    @JsonIgnore
    public User getFrom() {
        return from;
    }

    @JsonProperty(value = "from")
    public void setFrom(User from) {
        this.from = from;
        this.author = from.getName();
    }

    @Override
    @JsonProperty(value = "time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+2")
    public void setTimestamp(Date timestamp) {
        super.setTimestamp(timestamp);
    }

    @Override
    @JsonProperty(value = "timestamp")
    public Date getTimestamp() {
        return super.getTimestamp();
    }

    @Override
    @JsonProperty(value = "name")
    public void setAuthor(String author) {
        super.setAuthor(author);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonIgnore
    public int getFsId() {
        return fsId;
    }

    @JsonProperty(value = "id")
    public void setFsId(int id) {
        this.fsId= id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class User {
        String name;
        int id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }
}
