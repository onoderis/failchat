package failchat.twitch;

import failchat.core.ViewersCounter;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.net.URL;

public class TwitchViewersCounter implements ViewersCounter {
    private static final String VIEWERS_API_URL = "https://api.twitch.tv/kraken/streams/";
    private String channel;
    private int viewers = -1;

    public TwitchViewersCounter(String channel) {
        this.channel = channel;
    }

    @Override
    public int getViewersCount() {
        return viewers;
    }

    @Override
    public void updateViewersCount() {
        try {
            URL globalEmotesUrl = new URL(VIEWERS_API_URL + channel);
            String rawJS = IOUtils.toString(globalEmotesUrl.openConnection().getInputStream());
            JSONObject responseObj = new JSONObject(rawJS);
            viewers = responseObj.getJSONObject("stream").getInt("viewers");
        } catch (Exception e) {
            viewers = -1;
        }
    }
}
