package failchat.twitch;

import failchat.core.MessageHandler;
import failchat.core.SmileManager;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitchSmileHandler implements MessageHandler<TwitchMessage> {
    private static final Logger logger = Logger.getLogger(TwitchSmileHandler.class.getName());

    private Pattern smileIdsPattern = Pattern.compile("(\\d+):");

    @Override
    public void handleMessage(TwitchMessage message) {
        if (message.getUsedSmiles() == null || message.getUsedSmiles().equals("")) {
            return;
        }

        ArrayList<TwitchSmile> usedSmiles = new ArrayList<>();
        Matcher m = smileIdsPattern.matcher(message.getUsedSmiles());
        while (m.find()) {
            TwitchSmile smile = TwitchSmileInfoLoader.getSmile(Integer.parseInt(m.group(1)));
            if (smile != null) { // на случай если смайлы ещё не загрузились
                usedSmiles.add(smile);
            }
        }

        usedSmiles.forEach((smile) -> {
            Pattern smilePattern;
            if (smile.isRegex()) {
                smilePattern = Pattern.compile(smile.getCode());
            } else {
                smilePattern = Pattern.compile("(?<=\\b)" + smile.getCode() + "(?=\\b)");
            }
            Matcher m2 = smilePattern.matcher(message.getText());
            int position = 0;
            while (m2.find(position)) {
                position = m2.start();
                message.setText(m2.replaceFirst(message.addSmile(smile)));
                m2 = smilePattern.matcher(message.getText());
            }
            SmileManager.cacheSmile(smile);
        });
    }
}
