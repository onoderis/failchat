package failchat.twitch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.SmileManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class TwitchSmileInfoLoader {
    private static final Logger logger = Logger.getLogger(TwitchSmileInfoLoader.class.getName());
    private static final String GLOBAL_EMOTICONS = "https://twitchemotes.com/api_cache/v2/global.json";
    private static final String SER_FILENAME_GLOBAL = "twitch-g";
    private static final String SER_FILENAME_SUBS = "twitch-s";

    private static Map<Integer, SmileSet> smileSets = new HashMap<>();

    public static void loadGlobalSmilesInfo() {
        boolean loaded = false;
        try {
            URL globalEmotesUrl = new URL(GLOBAL_EMOTICONS);
            String rawJS = IOUtils.toString(globalEmotesUrl.openConnection().getInputStream());
            ObjectMapper objectMapper = new ObjectMapper();
            Document doc = objectMapper.readValue(rawJS, Document.class);
            for (HashMap.Entry<String, TwitchSmile> e : doc.getEmotes().entrySet()) {
                e.getValue().setCode(e.getKey());
            }
            smileSets.put(0, new SmileSet(0, doc.getEmotes()));
            loaded = true;
        } catch (IOException e) {
            logger.warning("Can't load twitch global smiles");
        }

        // serialization / deserialization
        if (!loaded) {
            Object o = SmileManager.deserialize(SER_FILENAME_GLOBAL);
            if (o != null) {
                //noinspection unchecked
                smileSets = (Map<Integer, SmileSet>) o;
            }
        }
        else {
            SmileManager.serialize(smileSets, SER_FILENAME_GLOBAL);
        }
        if (smileSets.get(0) != null) {
            logger.info("Twitch global smiles: " + smileSets.get(0).getSmiles().size());
        }
        logger.info("Twitch smile sets: " + smileSets.size());
    }

    public static SmileSet getSmileSet(int id) {
        return smileSets.get(id);
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
