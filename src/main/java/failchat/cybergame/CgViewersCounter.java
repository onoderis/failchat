package failchat.cybergame;

import failchat.core.ViewersCounter;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class CgViewersCounter implements ViewersCounter {
    private static final String CYBERGAME_API_URL = "http://api.cybergame.tv/w/streams2.php?channel=";
    private String channel;
    private int viewers = -1;

    public CgViewersCounter(String channel) {
        this.channel = channel;
    }

    @Override
    public int getViewersCount() {
        return viewers;
    }

    @Override
    public void updateViewersCount() {
        try {
            URL url = new URL(CYBERGAME_API_URL + channel);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            if (con.getResponseCode() != 200) {
                throw new Exception();
            }
            JSONObject responseObj = new JSONObject(IOUtils.toString(con.getInputStream()));
            if (responseObj.getInt("online") != 1) {
                throw new Exception();
            }
            viewers = responseObj.getInt("viewers");
        } catch (Exception e) {
            viewers = -1;
        }
    }
}
