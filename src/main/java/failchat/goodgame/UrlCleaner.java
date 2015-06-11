package failchat.goodgame;

import failchat.core.MessageHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlCleaner implements MessageHandler<GGMessage> {
    private final Pattern urlHtml = Pattern.compile("<a target=\"_blank\" rel=\"nofollow\" href=\\\"(.*)\\\">\\1<\\/a>");

    @Override
    public void handleMessage(GGMessage message) {
        Matcher m = urlHtml.matcher(message.getText());
        while (m.find()) {
            String url = m.group(1);
            message.setText(m.replaceFirst(url));
            m = urlHtml.matcher(message.getText());
        }
    }
}
