package failchat.twitch;

import failchat.core.MessageHandler;
import failchat.core.SmileManager;

import java.util.logging.Logger;
import java.util.regex.Matcher;

public class TwitchSmileHandler implements MessageHandler<TwitchMessage> {
    private static final Logger logger = Logger.getLogger(TwitchSmileHandler.class.getName());

    @Override
    public void handleMessage(TwitchMessage message) {
        for (int smileSetId : message.getEmoteSets()) {
            SmileSet smileSet = TwitchSmileInfoLoader.getSmileSet(smileSetId);
            if (smileSet == null) {
                logger.warning("Smile set not loaded: " + smileSetId);
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
