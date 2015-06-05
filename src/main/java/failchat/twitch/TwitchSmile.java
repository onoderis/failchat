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
    private Pattern pattern;

    public TwitchSmile() {
        this.source = Source.TWITCH;
    }

    @Override
    @JsonProperty (value = "regex")
    public void setCode(String code) {
        this.code = code;
        pattern = Pattern.compile(code);
    }

    @Override
    @JsonProperty (value = "url")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        Matcher m = fileNamePattern.matcher(imageUrl);
        if (m.find()) {
            this.fileName = m.group(1);
        }
    }

    public Pattern getPattern() {
        return pattern;
    }
}
