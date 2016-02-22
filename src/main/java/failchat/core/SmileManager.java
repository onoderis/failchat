package failchat.core;

import failchat.funstream.FsSmileInfoLoader;
import failchat.goodgame.GGSmileInfoLoader;
import failchat.twitch.TwitchSmileInfoLoader;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сериализует и десериализует списки смайлов в /smiles/list/{name}.ser
 * Кеширует смайлы в /smiles/{source}/{filename_from_url}
*/
public class SmileManager {
    public static Path SMILES_DIR = Bootstrap.workDir.resolve("smiles");
    public static Path SMILES_DIR_REL = Paths.get("../../smiles"); //for browser
    public static Path SMILE_LIST_DIR = SMILES_DIR.resolve("list");

    private static final Logger logger = Logger.getLogger(SmileManager.class.getName());

    public static void loadSmilesInfo() {
        try {
            Files.createDirectories(SMILE_LIST_DIR);
        } catch (IOException e) {
            logger.warning("Can't create smile list directory");
            logger.log(Level.WARNING, "Something goes wrong...", e);
            return;
        }
        FsSmileInfoLoader.loadSmilesInfo();
        GGSmileInfoLoader.loadSmilesInfo();
        TwitchSmileInfoLoader.loadSmilesInfo();
    }

    public static void serialize(Object o, String name) {
        try (FileOutputStream fileOut = new FileOutputStream(SMILE_LIST_DIR.resolve(name + ".ser").toFile());
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(o);
            logger.info(name + " smiles serialized");
        } catch (IOException e) {
            logger.warning("Can't serialize " + name + " smiles");
        }
    }

    public static Object deserialize(String name) {
        try (FileInputStream fileIn = new FileInputStream(SMILE_LIST_DIR.resolve(name + ".ser").toFile());
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            Object o = in.readObject();
            logger.info(name + " smiles deserialized");
            return o;
        } catch (IOException e) {
            logger.warning("Can't deserialize " + name + " smiles");
            return null;
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Something goes wrong...", e);
            return null;
        }
    }

    public static boolean cacheSmile(Smile smile) {
        if (smile.isCached()) {
            return true;
        }
        Path filePath = SMILES_DIR.resolve(smile.getSource().getLowerCased()).resolve(smile.getFileName());

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
            logger.log(Level.WARNING, String.format("Cant't download %s smile %s from url %s", smile.source.getLowerCased(),
                    smile.getCode(), smile.getImageUrl()), e);
            return false;
        }
        return true;
    }
}
