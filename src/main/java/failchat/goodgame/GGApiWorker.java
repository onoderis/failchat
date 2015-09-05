package failchat.goodgame;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class GGApiWorker {
    private static final String GG_STREAM_API_URL = "http://goodgame.ru/api/getchannelstatus?fmt=json&id=";

    public static int getChannelIdByName(String channel) {
        JSONObject obj = apiRequest(channel);
        try {
            return obj != null ? obj.getJSONObject((String) obj.keys().next()).getInt("stream_id") : -1;
        } catch (JSONException e) {
            return -1;
        }
    }

    public static int getViewersCount(String channel) {
        JSONObject obj = apiRequest(channel);
        try {
            return obj != null ? obj.getJSONObject((String) obj.keys().next()).getInt("viewers") : -1;
        } catch (JSONException e) {
            return -1;
        }
    }

    private static JSONObject apiRequest(String channel) {
        try {
            URL url = new URL(GG_STREAM_API_URL + channel);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() != 200) {
                throw new Exception();
            }
            String response = IOUtils.toString(connection.getInputStream());
            return new JSONObject(response);
        } catch (Exception e) {
            return null;
        }
    }
}
