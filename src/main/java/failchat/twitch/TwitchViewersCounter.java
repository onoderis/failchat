package failchat.twitch;

import failchat.core.ViewersCounter;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TwitchViewersCounter implements ViewersCounter {

    private static final Logger LOG = Logger.getLogger(TwitchViewersCounter.class.getName());
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
            URL apiUrl = new URL(VIEWERS_API_URL + channel);
            URLConnection urlConnection = apiUrl.openConnection();
            //todo token shouldn't be hardcoded
            urlConnection.setRequestProperty("Client-ID", "o1flir755whtdb29otu0wekaif6mj5");
            try (InputStream inputStream = urlConnection.getInputStream()) {
                String rawResponse = IOUtils.toString(inputStream);
                JSONObject parsedResponse = new JSONObject(rawResponse);
                viewers = parsedResponse.getJSONObject("stream").getInt("viewers");
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, e, () -> "Failed to get twitch viewers count for channel '" + channel + "'");
            viewers = -1;
        }
    }
}
