package failchat.twitch;

import failchat.core.MessageHandler;
import failchat.core.SmileManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitchSmileHandler implements MessageHandler<TwitchMessage> {
    private static final Logger logger = Logger.getLogger(TwitchSmileHandler.class.getName());

    private Map<Integer, SmileSet> smileSets;
    private Pattern smilesPattern;


    public TwitchSmileHandler() {
        smileSets = new HashMap<>();
        SmileSet globalSmiles = TwitchSmileInfoLoader.loadGlobalSmiles();
        smileSets.put(globalSmiles.getId(), globalSmiles);
        //TODO: загрузка смайлов сабскрайберов в отдельном треде
    }

    @Override
    public void handleMessage(TwitchMessage message) {
        for (int smileSetId : message.getEmoteSets()) {
            SmileSet smileSet = smileSets.get(smileSetId);
            if (smileSet == null) {
//                logger.warning("Smile set not loaded: " + smileSetId);
                continue;
            }

            Matcher m = smileSet.getPattern().matcher(message.getText());
            while (m.find()) {
                String code = m.group();
                TwitchSmile smile = smileSet.getSmiles().get(code);
                String num = message.addSmile(smile);
                SmileManager.cacheSmile(smile);
                message.setText(message.getText().replaceFirst(code, num));
            }
        }
    }

}
