package failchat.twitch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.Configurator;
import failchat.core.SmileManager;
import failchat.core.Source;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TwitchSmileInfoLoader {
    private static final Logger logger = Logger.getLogger(TwitchSmileInfoLoader.class.getName());
    private static final String EMOTICONS_URL = "https://api.twitch.tv/kraken/chat/emoticon_images";

    private static Map<Integer, TwitchSmile> smileMap;

    public static void loadSmilesInfo() {
        boolean updated = Configurator.config.getLong("twitch.smiles.updated") + Configurator.config.getLong("smiles.updatingDelay") > System.currentTimeMillis();
        if (updated) {
            //noinspection unchecked
            smileMap = (Map<Integer, TwitchSmile>)SmileManager.deserialize(Source.TWITCH.getLowerCased());
        }
        if (smileMap == null) {
            smileMap = loadSmiles();
            if (smileMap == null) {
                if (!updated) {
                    //noinspection unchecked
                    smileMap = (Map<Integer, TwitchSmile>)SmileManager.deserialize(Source.TWITCH.getLowerCased());
                }
            } else {
                SmileManager.serialize(smileMap, Source.TWITCH.getLowerCased());
                Configurator.config.setProperty("twitch.smiles.updated", System.currentTimeMillis());
            }
        }
        if (smileMap != null) {
            logger.info("Twitch smiles: " + smileMap.size());
        } else {
            logger.warning("Twitch smiles not loaded");
        }
    }

    public static TwitchSmile getSmile(int id) {
        return smileMap != null ? smileMap.get(id) : null;
    }

    private static Map<Integer, TwitchSmile> loadSmiles() {
        try {
            URL globalEmotesUrl = new URL(EMOTICONS_URL);
            String rawJS = IOUtils.toString(globalEmotesUrl.openConnection().getInputStream());
            ObjectMapper objectMapper = new ObjectMapper();
            Document doc = objectMapper.readValue(rawJS, Document.class);
            Map<Integer, TwitchSmile> smiles = new HashMap<>();
            for (TwitchSmile sm : doc.getEmoticons()) {
                smiles.put(sm.getId(), sm);
            }
            return smiles;
        } catch (IOException e) {
            logger.warning("Can't load twitch smiles");
            return null;
        }
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
