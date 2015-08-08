package failchat.twitch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Smile;
import failchat.core.SmileManager;
import failchat.core.Source;

import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TwitchSmile extends Smile {
    public static final String SMILE_IMG_FORMAT = ".png";

    private static final String LOCATION_URL = "http://static-cdn.jtvnw.net/emoticons/v1/";
    private static final String LOCATION_URL_END = "/1.0";
    private static final Pattern regexCodePattern = Pattern.compile("[a-zA-Z0-9_]++");

    protected int id;

    public TwitchSmile() {
        source = Source.TWITCH;
    }

    public TwitchSmile(int id, String code) {
        this.id = id;
        this.code = code;
        source = Source.TWITCH;
    }

    @Override
    public void setCode(String code) {
        if (!regexCodePattern.matcher(code).matches()) {
            this.code = code.replace("\\&lt\\;", "<").replace("\\&gt\\;", ">"); //replace html entity for < >
        }
        else {
            this.code = code;
        }
    }

    @Override
    public String getCachePath() {
        return SmileManager.SMILES_DIR_REL.resolve(Source.TWITCH.getLowerCased())
                .resolve(getFileName()).toString().replace('\\', '/');
    }

    @JsonIgnore
    public int getId() {
        return id;
    }

    @JsonProperty (value = "id")
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getImageUrl() {
        return LOCATION_URL + id + LOCATION_URL_END;
    }

    @Override
    public String getFileName() {
        return id + SMILE_IMG_FORMAT;
    }
}
