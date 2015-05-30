package failchat.sc2tv;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.AbstractSmileInfoLoader;
import failchat.core.Smile;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sc2tvSmileInfoLoader extends AbstractSmileInfoLoader {
    public static final String SC2TV_SMILES_DIR_URL = "http://chat.sc2tv.ru/img/"; // используется для генерации url

    private static final Logger logger = Logger.getLogger(Sc2tvSmileInfoLoader.class.getName());
    private static final String SMILE_FILE_EXTENSION = ".png";
    private static final String SMILE_ARRAY_PATTERN = "var smiles=(\\[.*?\\]);"; // для извлечения JSON'а со смайлами из Smiles.js
    private static URL smilesJsUrl;

    private ObjectMapper objectMapper = new ObjectMapper();

    public Sc2tvSmileInfoLoader() {
        try {
            smilesJsUrl = new URL("http://chat.sc2tv.ru/js/smiles.js");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadSmilesInfo() {
        try {
            HttpURLConnection con = (HttpURLConnection) smilesJsUrl.openConnection();
            con.setRequestProperty("User-Agent", "failchat client");
            String rawJSstr = IOUtils.toString(con.getInputStream());
            Pattern p = Pattern.compile(SMILE_ARRAY_PATTERN);
            Matcher m = p.matcher(rawJSstr);
            if (!m.find()) {
                return;
            }
            String smilesJS = m.group(1);
            //TODO: Посмотреть можно ли как-нибудь парсить массив JSON сразу в Map
            List<Smile> smileList = objectMapper.readValue(smilesJS, new TypeReference<List<Sc2tvSmile>>() {});
            smiles = new HashMap<>();
            for (Smile s: smileList) {
                smiles.put(s.getCode(), s);
            }
            logger.info("Sc2tv smiles found: " + smileList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getFileExtension() {
        return SMILE_FILE_EXTENSION;
    }
}
