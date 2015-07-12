package failchat.sc2tv;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.SmileManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc2tvSmileInfoLoader {
    private static final Logger logger = Logger.getLogger(Sc2tvSmileInfoLoader.class.getName());
    private static final String SMILE_ARRAY_PATTERN = "var smiles=(\\[.*?\\]);"; // для извлечения JSON'а со смайлами из Smiles.js
    private static final String SMILES_JS_URL_STR = "http://chat.sc2tv.ru/js/smiles.js";
    private static final String SER_FILENAME = "sc2tv";

    private static Map<String, Sc2tvSmile> smiles = new HashMap<>();

    public static void loadSmilesInfo() {
        // try to load from net
        try {
            URL smilesJsUrl = new URL(SMILES_JS_URL_STR);
            HttpURLConnection con = (HttpURLConnection) smilesJsUrl.openConnection();
            con.setRequestProperty("User-Agent", "failchat client");
            String rawJSstr = IOUtils.toString(con.getInputStream());
            Pattern p = Pattern.compile(SMILE_ARRAY_PATTERN);
            Matcher m = p.matcher(rawJSstr);
            if (!m.find()) {
                throw new IOException();
            }
            String smilesJS = m.group(1);
            ObjectMapper objectMapper = new ObjectMapper();
            List<Sc2tvSmile> smileList = objectMapper.readValue(smilesJS, new TypeReference<List<Sc2tvSmile>>() {});
            Map<String, Sc2tvSmile> smilesMap = new HashMap<>();
            for (Sc2tvSmile s: smileList) {
                smilesMap.put(s.getCode(), s);
            }
            smiles = smilesMap;
            logger.info("Sc2tv smiles downloaded");
        } catch (IOException e) {
            logger.warning("Can't load sc2tv smiles");
        }

        // serialization / deserialization
        if (smiles.isEmpty()) {
            Object o = SmileManager.deserialize(SER_FILENAME);
            if (o != null) {
                //noinspection unchecked
                smiles = (Map<String, Sc2tvSmile>) o;
            }
        }
        else {
            SmileManager.serialize(smiles, SER_FILENAME);
        }

        logger.info("Sc2tv smiles: " + smiles.size());
    }

    public static Sc2tvSmile getSmile(String code) {
        return smiles.get(code);
    }
}
