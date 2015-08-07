package failchat.funstream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import failchat.core.Source;

public class FsMessage extends failchat.core.Message {
    protected  FsChatClient.User from; //for SourceFilter and HighlightHandler
    protected  FsChatClient.User to; //for HighlightHandler

    FsMessage() {}

    FsMessage (FsChatClient.Message message) {
        this.author = message.getFrom().getName();
        this.timestamp = message.getTimestamp();
        this.text = message.getText();
        this.source = Source.SC2TV;

        this.from = message.getFrom();
        this.to = message.getTo();
    }

    @JsonIgnore
    public FsChatClient.User getTo() {
        return to;
    }

    public void setTo(FsChatClient.User to) {
        this.to = to;
    }

    @JsonIgnore
    public FsChatClient.User getFrom() {
        return from;
    }

    public void setFrom(FsChatClient.User from) {
        this.from = from;
    }
}
