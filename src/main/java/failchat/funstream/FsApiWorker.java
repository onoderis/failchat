package failchat.funstream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FsApiWorker {
    private static final Logger logger = Logger.getLogger(FsApiWorker.class.getName());
    private static final String apiUrl = "http://funstream.tv/api/";

    public static int getChannelIdByName(String channelName) {
        try {
            String response = sendRequest("user", new JSONObject().put("name", channelName));
            if (response != null) {
                return new JSONObject(response).getInt("id");
            }
            else {
                return -1;
            }
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Something goes wrong...", e);
            return -1;
        }
    }

    public static Map<String, FsSmile> loadSmiles() {
        String response = sendRequest("smile", null);
        if (response == null) {
            logger.warning("Can't load funstream smiles");
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<List<FsSmile>> smileLists = objectMapper.readValue(response, new TypeReference<List<List<FsSmile>>>() {});
            Map<String, FsSmile> smileMap = new HashMap<>();
            smileLists.forEach((list) -> {
                list.forEach((smile) -> {
                    smileMap.put(smile.getCode(), smile);
                });
            });
            return smileMap;
        } catch (IOException e) {
            return null;
        }
    }

    private static String sendRequest(String apiPath, JSONObject requestBody) {
        try {
            URL url = new URL(apiUrl + apiPath);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "failchat client");
            con.setRequestProperty("Accept", "application/json; version=1.0");

            if (requestBody != null) {
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(requestBody.toString());
                wr.flush();
                wr.close();
            }

            if (con.getResponseCode() != 200) {
                return null;
            }
            return IOUtils.toString(con.getInputStream());
        } catch (Exception e) {
            return null;
        }

    }
}
