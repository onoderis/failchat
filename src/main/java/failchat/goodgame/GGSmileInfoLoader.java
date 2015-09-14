package failchat.goodgame;

import failchat.core.Configurator;
import failchat.core.SmileManager;
import failchat.core.Source;

import java.util.Map;
import java.util.logging.Logger;

public class GGSmileInfoLoader {
    private static final Logger logger = Logger.getLogger(GGSmileInfoLoader.class.getName());

    private static Map<String, GGSmile> smileMap;

    public static void loadSmilesInfo() {
        boolean updated = Configurator.config.getLong("goodgame.smiles.updated") + Configurator.config.getLong("smiles.updatingDelay") > System.currentTimeMillis();
        if (updated) {
            //noinspection unchecked
            smileMap = (Map<String, GGSmile>)SmileManager.deserialize(Source.GOODGAME.getLowerCased());
        }
        if (smileMap == null) {
            smileMap = GGApiWorker.loadSmiles();
            if (smileMap == null) {
                if (!updated) {
                    //noinspection unchecked
                    smileMap = (Map<String, GGSmile>)SmileManager.deserialize(Source.GOODGAME.getLowerCased());
                }
            } else {
                SmileManager.serialize(smileMap, Source.GOODGAME.getLowerCased());
                Configurator.config.setProperty("goodgame.smiles.updated", System.currentTimeMillis());
            }
        }
        if (smileMap != null) {
            logger.info("Goodgame smiles: " + smileMap.size());
        } else {
            logger.warning("Goodgame smiles not loaded");
        }
    }

    public static GGSmile getSmile(String code) {
        return smileMap != null ? smileMap.get(code) : null;
    }
}
