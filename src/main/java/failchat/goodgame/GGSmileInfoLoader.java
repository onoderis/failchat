package failchat.goodgame;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.SmileManager;
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
    private static final String SER_FILENAME = "gg";

    private static Map<String, GGSmile> smiles = new HashMap<>();

    public static void loadSmilesInfo() {
        // try to load from net
        try {
            String rawJS = IOUtils.toString(new URL(SMILES_JS_URL).openConnection().getInputStream());
            Matcher m = SMILES_ARRAY.matcher(rawJS);
            if (!m.find()) {
                logger.warning("Can't extract goodgame smiles array from raw .js");
                throw new IOException();
            }
            String rawJSSmiles = m.group(1);
            ObjectMapper objectMapper = new ObjectMapper();
            List<GGSmile> smileList = objectMapper.readValue(rawJSSmiles, new TypeReference<List<GGSmile>>() {});

            //create animated instances
            smileList.forEach((smile) -> {
                if (smile.animated) {
                    GGSmile aSmile = new GGSmile();
                    aSmile.setCode(smile.getCode());
                    aSmile.setAnimated(true);
                    aSmile.setPremium(true);
                    smile.setAnimatedInstance(aSmile);
                    smile.setAnimated(false);
                }
            });

            // list to map
            Map<String, GGSmile> smileMap = new HashMap<>();
            for (GGSmile smile : smileList) {
                smileMap.put(smile.getCode(), smile);
//                System.out.println("Smile: " + smile.getCode() + ", premium " + smile.premium + ", animated " + smile.animated);
//                if (smile.getAnimatedInstance() != null) {
//                    System.out.println("animated instance: " + smile.getAnimatedInstance().getImageUrl() + " "  + smile.getAnimatedInstance().getCachePath());
//                }
            }
            smiles = smileMap;
        } catch (IOException e) {
            logger.warning("Can't load goodgame smiles");
        }

        // serialization / deserialization
        if (smiles.isEmpty()) {
            Object o = SmileManager.deserialize(SER_FILENAME);
            if (o != null) {
                //noinspection unchecked
                smiles = (Map<String, GGSmile>) o;
            }
        }
        else {
            SmileManager.serialize(smiles, SER_FILENAME);
        }
        logger.info("Goodgame smiles: " + smiles.size());
    }

    public static GGSmile getSmile(String code) {
        return smiles.get(code);
    }
}
