package failchat.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Image {
    private int objectNumber;
    private String imageLink;

    public Image(String imageLink) {
        this.imageLink = imageLink;
    }

    public int getObjectNumber() {
        return objectNumber;
    }

    public void setObjectNumber(int objectNumber) {
        this.objectNumber = objectNumber;
    }

    @JsonProperty (value = "imgUrl")
    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }
}
