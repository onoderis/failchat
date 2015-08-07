package failchat.twitch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.SmileManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TwitchSmileInfoLoader {
    private static final Logger logger = Logger.getLogger(TwitchSmileInfoLoader.class.getName());
    private static final String EMOTICONS_URL = "http://api.twitch.tv/kraken/chat/emoticon_images";
    private static final String SER_FILENAME = "twitch";

    private static Map<Integer, TwitchSmile> smiles = new HashMap<>();

    public static void loadGlobalSmilesInfo() {
        try {
            URL globalEmotesUrl = new URL(EMOTICONS_URL);
            String rawJS = IOUtils.toString(globalEmotesUrl.openConnection().getInputStream());
            ObjectMapper objectMapper = new ObjectMapper();
            Document doc = objectMapper.readValue(rawJS, Document.class);
            Map<Integer, TwitchSmile> smilesMap = new HashMap<>();
            for (TwitchSmile sm : doc.getEmoticons()) {
                smilesMap.put(sm.getId(), sm);
            }
            smiles = smilesMap;
        } catch (IOException e) {
            logger.warning("Can't load twitch smiles");
        }

        // serialization / deserialization
        if (smiles.isEmpty()) {
            Object o = SmileManager.deserialize(SER_FILENAME);
            if (o != null) {
                //noinspection unchecked
                smiles = (Map<Integer, TwitchSmile>) o;
            }
        }
        else {
            SmileManager.serialize(smiles, SER_FILENAME);
        }
        logger.info("Twitch smiles: " + smiles.size());
    }

    public static TwitchSmile getSmile(int id) {
        return smiles.get(id);
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Document {
        private List<TwitchSmile> emoticons;

        public List<TwitchSmile> getEmoticons() {
            return emoticons;
        }

        public void setEmoticons(List<TwitchSmile> emoticons) {
            this.emoticons = emoticons;
        }
    }
}
