package failchat.funstream;

import failchat.core.SmileManager;
import failchat.core.Source;

import java.util.Map;
import java.util.logging.Logger;

public class FsSmileInfoLoader {
    private static final Logger logger = Logger.getLogger(FsSmileInfoLoader.class.getName());
    private static Map<String, FsSmile> smileMap;

    public static void loadSmilesInfo() {
        smileMap = FsApiWorker.loadSmilesInfo();
        if (smileMap == null) {
            //noinspection unchecked
            smileMap = (Map<String, FsSmile>)SmileManager.deserialize(Source.FUNSTREAM.getLowerCased());
        } else {
            SmileManager.serialize(smileMap, Source.FUNSTREAM.getLowerCased());
        }
        if (smileMap != null) {
            logger.info("Funstream smiles: " + smileMap.size());
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
