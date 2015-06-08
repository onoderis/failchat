package failchat.core;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Кеширует смайлы в /smiles/{source}/{filename_from_url}
*/
public class SmileManager {

    public static Path SMILES_DIR = Bootstrap.workDir.resolve("smiles");
    public static Path SMILES_DIR_REL = Paths.get("../../smiles"); //for browser

    private static final Logger logger = Logger.getLogger(SmileManager.class.getName());

    private static volatile SmileManager instance;

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


    public static boolean cacheSmile(Smile smile) {
        if (smile.isCached()) {
            return true;
        }
        Path filePath = SMILES_DIR.resolve(smile.getSource().toString().toLowerCase()).resolve(smile.getFileName());

        //if smile already downloaded
        if (Files.exists(filePath)) {
            logger.fine("Smile already exists: " + smile.getCode());
            smile.setCached(true);
            return true;
        }

        //downloading smile
        try {
            URLConnection con =  new URL(smile.getImageUrl()).openConnection();
            con.setRequestProperty("User-Agent", "failchat client");
            FileUtils.copyInputStreamToFile(con.getInputStream(), filePath.toFile());
            smile.setCached(true);
            logger.fine("Smile downloaded: " + filePath.toFile().toString());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
