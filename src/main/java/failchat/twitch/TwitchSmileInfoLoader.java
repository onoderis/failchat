package failchat.twitch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.Smile;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitchSmileInfoLoader {
    private static final Logger logger = Logger.getLogger(TwitchSmileInfoLoader.class.getName());
    private static final String CHANNEL_EMOTICONS = "https://api.twitch.tv/kraken/chat/";
    private static final String CHANNEL_EMOTICONS_END = "/emoticons";
    private static final String SMILE_ARRAY_PATTERN = "\"emoticons\":(\\[.*\\])";

    public static Map<String, Smile> loadSmilesInfo(String channelName) {
        try {
            URL channelEmoticonsUrl = new URL(CHANNEL_EMOTICONS + channelName + CHANNEL_EMOTICONS_END);
            HttpURLConnection con = (HttpURLConnection) channelEmoticonsUrl.openConnection();
            String rawJSstr = IOUtils.toString(con.getInputStream());
            Pattern p = Pattern.compile(SMILE_ARRAY_PATTERN);
            Matcher m = p.matcher(rawJSstr);
            if (!m.find()) {
                return null;
            }
            String smilesJS = m.group(1);
            ObjectMapper objectMapper = new ObjectMapper();
            List<Smile> smileList = objectMapper.readValue(smilesJS, new TypeReference<List<TwitchSmile>>() {});
            Map<String, Smile> smilesMap = new HashMap<>();
            for (Smile s: smileList) {
                smilesMap.put(s.getCode(), s);
            }
            logger.info("Twitch smiles found: " + smileList.size());
            return smilesMap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
