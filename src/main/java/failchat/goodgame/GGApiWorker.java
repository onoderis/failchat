package failchat.goodgame;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GGApiWorker {
    private static final Logger logger = Logger.getLogger(GGApiWorker.class.getName());
    private static final String GG_STREAM_API_URL = "http://goodgame.ru/api/getchannelstatus?fmt=json&id=";
    private static final String SMILES_JS_URL = "http://goodgame.ru/js/minified/global.js";
    private static final Pattern SMILES_ARRAY = Pattern.compile("Smiles : (\\[.+?\\]),");
    private static final Pattern CHANNEL_SMILES_ARRAY = Pattern.compile("Channel_Smiles : (\\{.+?\\}]}),");

    public static Map<String, GGSmile> loadSmiles() {
        try {
            //global smiles
            String rawJS = IOUtils.toString(new URL(SMILES_JS_URL).openConnection().getInputStream());
            Matcher m = SMILES_ARRAY.matcher(rawJS);
            if (!m.find()) {
                logger.warning("Can't extract goodgame smiles array from raw .js");
                throw new IOException();
            }
            String rawJSSmiles = m.group(1);
            ObjectMapper objectMapper = new ObjectMapper();
            List<GGSmile> smileList = objectMapper.readValue(rawJSSmiles, new TypeReference<List<GGSmile>>() {});

            // list to map
            Map<String, GGSmile> smileMap = new HashMap<>();
            for (GGSmile smile : smileList) {
                smileMap.put(smile.getCode(), smile);
            }

            //channel smiles
            m = CHANNEL_SMILES_ARRAY.matcher(rawJS);
            if (!m.find()) {
                logger.warning("Can't extract goodgame channel smiles array from raw .js");
                throw new IOException();
            }
            rawJSSmiles = m.group(1);
            Map<String, List<GGSmile>> channelSmiles = objectMapper.readValue(rawJSSmiles, new TypeReference<Map<String, List<GGSmile>>>() {});
            channelSmiles.entrySet().forEach((entry) -> {
                for (GGSmile smile : entry.getValue()) {
                    if (smile.getTag().equals(GGSmile.INACTIVE_TAG)) { //skip smiles with inactive tag
                        continue;
                    }
                    smileMap.put(smile.getCode(), smile);
                }
            });

            //create animated instances
            smileMap.forEach((smileCode, smile) -> {
                if (smile.animated) {
                    GGSmile aSmile = new GGSmile();
                    aSmile.setCode(smile.getCode());
                    aSmile.setAnimated(true);
                    aSmile.setPremium(true);
                    aSmile.setBind(smile.getBind());
                    smile.setAnimatedInstance(aSmile);
                    smile.setAnimated(false);
                }
            });

            return smileMap;
        } catch (IOException e) {
            logger.warning("Can't load goodgame smiles");
            logger.log(Level.WARNING, e.getMessage(), e);
            return null;
        }
    }

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
            JSONObject iObj = obj.getJSONObject((String) obj.keys().next());
            return iObj.getString("status").equals("Live") ? iObj.getInt("viewers") : -1;
        } catch (Exception e) {
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
