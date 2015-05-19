package failchat.sc2tv;


import failchat.core.Message;
import failchat.core.MessageHandler;
import failchat.core.MessageSource;

import java.util.HashMap;
import java.util.Set;

/**
 Заменяет bbcode на html расметку в сообщениях sc2tv
*/

public class BBCodeHandler implements MessageHandler {

    static HashMap<String, String> rules = new HashMap<String, String>() {{
        put("\\[b\\]", "<b>");
        put("\\[/b\\]", "</b>");
        put("\\[url\\]", "");
        put("\\[/url\\]", "");
    }};

    @Override
    public void handleMessage(Message message) {
        if (message.getSource() != MessageSource.SC2TV) {
            return;
        }

        String messageText = message.getText();
        Set<String> set = rules.keySet();
        for (String pattern : set) {
            messageText = messageText.replaceAll(pattern, rules.get(pattern));
        }
        message.setText(messageText);
    }
}
