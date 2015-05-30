package failchat.core;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Кеширует смайлы в /smiles/{source}/{filename_from_url}
*/
public class SmileManager {

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
        if (smile.getCachePath() != null) {
            return true;
        }
        Path filePath = Bootstrap.workDir.resolve("smiles").resolve(smile.getSource().toString().toLowerCase()).resolve(smile.getFileName());
        //for browser
        Path relativePath = Paths.get("../../smiles").resolve(smile.getSource().toString().toLowerCase())
                .resolve(smile.getFileName());

        //if smile already downloaded
        if (Files.exists(filePath)) {
            logger.fine("Smile already exists: " + smile.getCode());
            smile.setCachePath(relativePath.toString());
            return true;
        }

        //downloading smile
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(smile.getImageUrl()).openConnection();
            con.setRequestProperty("User-Agent", "failchat client");
            FileUtils.copyInputStreamToFile(con.getInputStream(), filePath.toFile());
            smile.setCachePath(relativePath.toString());
            logger.fine("Smile downloaded: " + filePath.toFile().toString());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
