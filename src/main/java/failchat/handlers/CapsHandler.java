package failchat.handlers;

import failchat.core.Message;
import failchat.core.MessageHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обрабатывает сообщения с педупреждением за капс
 * <span class="red" title="CAPS MESSAGE">Предупреждение за CAPS / Abuse!</span> -> CAPS MESSAGE
 */
public class CapsHandler implements MessageHandler {
    private Pattern capsPattern = Pattern.compile("<span class=\"red\" title=\"(.*)\">Предупреждение за CAPS / Abuse\\!</span>");

    @Override
    public void handleMessage(Message message) {
        Matcher m = capsPattern.matcher(message.getText());
        if (m.find()) {
            message.setText(m.replaceAll("$1"));
        }
    }
}
