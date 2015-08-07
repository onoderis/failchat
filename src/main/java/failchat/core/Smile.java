package failchat.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Smile implements Serializable {
    protected String code; //smile code without ':' or regexp
    protected Source source;
    protected transient boolean cached;

    public Smile() {
        cached = false;
    }

    @JsonIgnore
    public String getImageUrl() {
        return null;
    }

    @JsonIgnore
    public Source getSource() {
        return source;
    }

    @JsonProperty (value = "code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @JsonProperty (value = "imgUrl")
    public String getCachePath() {
        return null;
    }

    @JsonIgnore
    public String getFileName() {
        return code;
    }

    @JsonIgnore
    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }
}

