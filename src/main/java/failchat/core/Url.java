package failchat.core;

/**
 * Класс, сериализующийся в json для отправки к локальному клиенту
 */
public class Url {
    private int objectNumber;
    private String url;
    private String domain;
    private String shortUrl;

    public Url (String url, String domain, String shortUrl) {
        this.url = url;
        this.domain = domain;
        this.shortUrl = shortUrl;
    }

    public int getObjectNumber() {
        return objectNumber;
    }

    public void setObjectNumber(int objectNumber) {
        this.objectNumber = objectNumber;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }
}
