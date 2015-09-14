package failchat.funstream;

import failchat.core.Configurator;
import failchat.core.SmileManager;
import failchat.core.Source;

import java.util.Map;
import java.util.logging.Logger;

public class FsSmileInfoLoader {
    private static final Logger logger = Logger.getLogger(FsSmileInfoLoader.class.getName());
    private static Map<String, FsSmile> smileMap;

    public static void loadSmilesInfo() {
        boolean updated = Configurator.config.getLong("sc2tv.smiles.updated") + Configurator.config.getLong("smiles.updatingDelay") > System.currentTimeMillis();
        if (updated) {
            //noinspection unchecked
            smileMap = (Map<String, FsSmile>)SmileManager.deserialize(Source.SC2TV.getLowerCased());
        }
        if (smileMap == null) {
            smileMap = FsApiWorker.loadSmiles();
            if (smileMap == null) {
                if (!updated) {
                    //noinspection unchecked
                    smileMap = (Map<String, FsSmile>)SmileManager.deserialize(Source.SC2TV.getLowerCased());
                }
            } else {
                SmileManager.serialize(smileMap, Source.SC2TV.getLowerCased());
                Configurator.config.setProperty("sc2tv.smiles.updated", System.currentTimeMillis());
            }
        }
        if (smileMap != null) {
            logger.info("Sc2tv smiles: " + smileMap.size());
        } else {
            logger.warning("Sc2tv smiles not loaded");
        }
    }

    public static FsSmile getSmile(String code) {
        if (smileMap == null) {
            return null;
        } else {
            return smileMap.get(code);
        }
    }

}
