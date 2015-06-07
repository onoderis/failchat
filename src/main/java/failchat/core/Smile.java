package failchat.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Smile {
    protected String code; //smile code or regexp
    protected String imageUrl;
    protected Source source;
    protected String cachePath; //url файла для браузера (если смайл уже в кеше)
    protected String fileName; //имя файла из imageUrl

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

    @JsonIgnore
    public void setSource(Source source) {
        this.source = source;
    }

    @JsonIgnore
    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }

    @JsonIgnore
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}

