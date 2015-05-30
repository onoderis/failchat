package failchat.core;

import failchat.sc2tv.Sc2tvSmileInfoLoader;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Инициализирует SmileInfoLoader'ы. Кеширует смайлы
 *
 * Смайлы кешируются в /smiles/{source}/{code.format}
*/
public class SmileManager {

    private static final Logger logger = Logger.getLogger(SmileManager.class.getName());

    private static volatile SmileManager instance;

    private Map<Source, AbstractSmileInfoLoader> smileInfoLoaders;

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

    private SmileManager() {
        smileInfoLoaders = new HashMap<>();
        smileInfoLoaders.put(Source.SC2TV, new Sc2tvSmileInfoLoader());
    }

    public void loadSmiles() {
        smileInfoLoaders.values().forEach(AbstractSmileInfoLoader::loadSmilesInfo);
    }


    public boolean cacheSmile(Smile smile) {
        if (smile.getCache() != null) {
            return true;
        }
        String fileName = smile.getCode() + smileInfoLoaders.get(smile.getSource()).getFileExtension();
        Path filePath = Bootstrap.workDir.resolve("smiles").resolve(smile.getSource().toString().toLowerCase()).resolve(fileName);
        //for browser
        Path relativePath = Paths.get("../../smiles").resolve(smile.getSource().toString().toLowerCase())
                .resolve(fileName);

        //if smile already downloaded
        if (Files.exists(filePath)) {
            logger.fine("Smile already exists: " + smile.getCode());
            smile.setCache(relativePath.toString());
            return true;
        }

        //downloading smile
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(smile.getImageUrl()).openConnection();
            con.setRequestProperty("User-Agent", "failchat client");
            FileUtils.copyInputStreamToFile(con.getInputStream(), filePath.toFile());
            smile.setCache(relativePath.toString());
            logger.fine("Smile downloaded: " + filePath.toFile().toString());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Map<String, Smile> getSmiles(Source source) {
        return smileInfoLoaders.get(source).getSmiles();
    }
}
