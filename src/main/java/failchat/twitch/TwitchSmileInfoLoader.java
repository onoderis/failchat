package failchat.twitch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.Smile;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

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
    private static final String GLOBAL_EMOTICONS = "https://twitchemotes.com/api_cache/v2/global.json";

    public static SmileSet loadGlobalSmiles() {
        try {
            URL globalEmotesUrl = new URL(GLOBAL_EMOTICONS);
            String rawJS = IOUtils.toString(globalEmotesUrl.openConnection().getInputStream());
            ObjectMapper objectMapper = new ObjectMapper();
            Document doc = objectMapper.readValue(rawJS, Document.class);
            for (HashMap.Entry<String, TwitchSmile> e : doc.getEmotes().entrySet()) {
                e.getValue().setCode(e.getKey());
            }
            logger.info("Twitch global smiles found: " + doc.getEmotes().size());
            return new SmileSet(0, doc.getEmotes());
        } catch (IOException e) {
            e.printStackTrace();
            logger.warning("Can't load global smiles");
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Document {
        private Map<String, TwitchSmile> emotes;

        public Map<String, TwitchSmile> getEmotes() {
            return emotes;
        }

        public void setEmotes(Map<String, TwitchSmile> emotes) {
            this.emotes = emotes;
        }
    }
}
