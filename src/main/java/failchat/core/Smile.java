package failchat.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class Smile {
    protected String code; //smile code or regexp
    protected boolean cached;

    public Smile() {
        cached = false;
    }

    @JsonIgnore
    public abstract String getImageUrl();

    public abstract Source getSource();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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

