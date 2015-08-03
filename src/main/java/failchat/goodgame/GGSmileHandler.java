package failchat.goodgame;

import failchat.core.MessageHandler;
import failchat.core.SmileManager;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GGSmileHandler implements MessageHandler<GGMessage> {
    private static final Logger logger = Logger.getLogger(GGSmileHandler.class.getName());

    private Pattern smileCodePattern = Pattern.compile("((?<=:)(\\w+)(?=:))");

    @Override
    public void handleMessage(GGMessage message) {
        Matcher matcher = smileCodePattern.matcher(message.getText());
        int position = 0; // чтобы не начинать искать сначала, если :something: найдено, но это не смайл
        while (matcher.find(position)) {
            String code = matcher.group();
            GGSmile smile = GGSmileInfoLoader.getSmile(code);
            if (smile != null) {
                if (message.isPremiumUser() && smile.getAnimatedInstance() != null) {
                    smile = smile.getAnimatedInstance();
                }
                if (!SmileManager.cacheSmile(smile)) {
                    position = matcher.start() + 1;
                    continue;
                }
                String num = message.addSmile(smile);

                //replace smile text for object
                int start = matcher.start();
                int end = matcher.end();
                StringBuilder sb = new StringBuilder(message.getText());
                sb.delete(start - 1, end + 1); // for ':'
                sb.insert(start - 1, num);
                message.setText(sb.toString());
                matcher = smileCodePattern.matcher(message.getText());
                position = start;
            } else {
                position = matcher.end();
            }
        }
    }
}
