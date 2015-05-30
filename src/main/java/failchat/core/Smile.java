package failchat.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Smile {
    protected String code;
    protected String imageUrl;
    protected Source source;
    protected String cache; //url файла (если смайл уже в кеше)

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

    public Source getSource() {
        return source;
    }

    //TODO: а надо ли оно? может сделать isCustom?
    @JsonIgnore
    public void setSource(Source source) {
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

