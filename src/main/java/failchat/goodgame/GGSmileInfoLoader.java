package failchat.goodgame;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GGSmileInfoLoader {
    public static final String SMILES_JS_URL = "http://goodgame.ru/js/minified/global.js";

    private static final Logger logger = Logger.getLogger(GGSmileInfoLoader.class.getName());
    private static final Pattern SMILES_ARRAY = Pattern.compile("Smiles : (\\[.+?\\]),");

    public static Map<String, GGSmile> loadSmilesInfo() {
        try {
            String rawJS = IOUtils.toString(new URL(SMILES_JS_URL).openConnection().getInputStream());
            Matcher m = SMILES_ARRAY.matcher(rawJS);
            if (!m.find()) {
                logger.warning("Can't extract goodgame smiles array from raw .js");
                return null;
            }
            String rawJSSmiles = m.group(1);
            ObjectMapper objectMapper = new ObjectMapper();
            List<GGSmile> smileList = objectMapper.readValue(rawJSSmiles, new TypeReference<List<GGSmile>>() {});
            Map<String, GGSmile> smileMap = new HashMap<>();
            // list to map
            for (GGSmile smile : smileList) {
                smileMap.put(smile.getCode(), smile);
            }
            logger.info("Goodgame smiles found: " + smileList.size());
            return smileMap;
        } catch (IOException e) {
            e.printStackTrace();
            logger.warning("Can't download goodgame smiles");
            return null;
        }

    }

}
