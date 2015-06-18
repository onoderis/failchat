package failchat.core;

/**
 * Класс, сериализующийся в json для отправки к локальному клиенту
 */
public class Link {
    private int objectNumber;
    private String link;
    private String domain;
    private String shortLink;

    public Link(String link, String domain, String shortLink) {
        this.link = link;
        this.domain = domain;
        this.shortLink = shortLink;
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

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getShortLink() {
        return shortLink;
    }

    public void setShortLink(String shortLink) {
        this.shortLink = shortLink;
    }
}
