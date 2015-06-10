package failchat.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Smile {
    protected String code; //smile code without ':' or regexp
    protected boolean cached;

    public Smile() {
        cached = false;
    }

    @JsonIgnore
    public abstract String getImageUrl();

    public abstract Source getSource();

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

