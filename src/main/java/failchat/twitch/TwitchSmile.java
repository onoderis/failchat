package failchat.twitch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Smile;
import failchat.core.Source;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TwitchSmile extends Smile {
    private static final Pattern fileNamePattern = Pattern.compile("\\/([^/]*?\\.png)");
    private static final String LOCATION_URL = "http://static-cdn.jtvnw.net/emoticons/v1/";
    private static final String LOCATION_URL_END = "/1.0";
    private static final String SMILE_IMG_FORMAT = ".png";

    private int id;

    public TwitchSmile() {

    }

    public TwitchSmile(int id, String code) {
        this.id = id;
        this.code = code;
    }

    @Override
    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        Matcher m = fileNamePattern.matcher(imageUrl);
        if (m.find()) {
            this.fileName = m.group(1);
        }
    }

    @Override
    public Source getSource() {
        return Source.TWITCH;
    }

    public int getId() {
        return id;
    }

    @JsonProperty (value = "image_id")
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getImageUrl() {
        return LOCATION_URL + id + LOCATION_URL_END;
    }

    @Override
    public String getFileName() {
        return code + SMILE_IMG_FORMAT;
    }
}
