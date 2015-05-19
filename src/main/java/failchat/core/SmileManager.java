package failchat.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.sc2tv.Sc2tvSmile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Парсит js в поисках новых смайлов, кеширует смайлы
 *
 * Смайлы кешируются в /smiles/{source}/{code.format}
*/
public class SmileManager {

    public static final String SC2TV_SMILES_DIR_URL = "http://chat.sc2tv.ru/img/";
    public static final String SMILE_FILE_EXTENSION= ".png";

    private static volatile SmileManager instance;

    private static final String SC2TV_PATTERN = "var smiles=(\\[.*?\\]);";
    private static URL sc2tv_smiles_url;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Smile> sct2tvSmiles;
    private Map<String, Smile> ggSmiles;
    private Map<String, Smile> twitchSmiles;

    private SmileManager() {
        try {
            sc2tv_smiles_url = new URL("http://chat.sc2tv.ru/js/smiles.js");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static SmileManager getInstance() {
        SmileManager localInstance = instance;
        if (localInstance == null) {
            synchronized (SmileManager.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new SmileManager();
                }
            }
        }
        return localInstance;
    }

    public void loadSmiles() {
        loadSc2tvSmiles();
    }

    private void loadSc2tvSmiles() {
        try {
            HttpURLConnection con = (HttpURLConnection) sc2tv_smiles_url.openConnection();
            con.setRequestProperty("User-Agent", "failchat client");
            String rawJSstr = IOUtils.toString(con.getInputStream());
            Pattern p = Pattern.compile(SC2TV_PATTERN);
            Matcher m = p.matcher(rawJSstr);
            if (!m.find()) {
                 return;
            }
            String smilesJS = m.group(1);
            List<Smile> smileList = objectMapper.readValue(smilesJS, new TypeReference<List<Sc2tvSmile>>() {});
            sct2tvSmiles = new HashMap<>();
            for (Smile s: smileList) {
                s.setSource(SmileSource.SC2TV);
                s.setCode(s.getCode().replaceAll("\\:", ""));
                sct2tvSmiles.put(s.code, s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean cacheSmile(Smile smile) {
        if (smile.getCache() != null) {
            return true;
        }
        String fileName = smile.getCode() + SMILE_FILE_EXTENSION;
        Path filePath = Bootstrap.workDir.resolve("smiles").resolve(smile.getSource().toString().toLowerCase()).resolve(fileName);
        //for browser
        Path relativePath = Paths.get("../../smiles").resolve(smile.getSource().toString().toLowerCase())
                .resolve(fileName);

        //if smile already downlaoded
        if (Files.exists(filePath)) {
            Logger.fine("Smile already exists: " + smile.getCode());
            smile.setCache(relativePath.toString());
            return true;
        }

        //downloading smile
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(smile.getImageUrl()).openConnection();
            con.setRequestProperty("User-Agent", "failchat client");
            FileUtils.copyInputStreamToFile(con.getInputStream(), filePath.toFile());
            smile.setCache(relativePath.toString());
            Logger.fine("Smile downloaded: " + filePath.toFile().toString());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Map<String, Smile> getSct2tvSmiles() {
        return sct2tvSmiles;
    }

    public Map<String, Smile> getGgSmiles() {
        return ggSmiles;
    }

    public Map<String, Smile> getTwitchSmiles() {
        return twitchSmiles;
    }


}
