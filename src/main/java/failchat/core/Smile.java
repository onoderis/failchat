package failchat.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Smile {
    protected String code;
    protected String imageUrl;
    protected SmileSource source;
    protected String cache;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public SmileSource getSource() {
        return source;
    }

    //TODO: а надо ли оно? может сделать isCustom?
    @JsonIgnore
    public void setSource(SmileSource source) {
        this.source = source;
    }

    @JsonIgnore
    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }
}

