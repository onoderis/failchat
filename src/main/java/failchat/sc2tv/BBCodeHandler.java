package failchat.sc2tv;


import failchat.core.Message;
import failchat.core.MessageHandler;

import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Удаляет bbcode разметку из sc2tv сообщений
 */

public class BBCodeHandler implements MessageHandler {
    //иногда ссылки приходят в другом виде: [url=http://i.imgur.com/xxx.png]http://i.imgur.com/xxx.png[/url]
    private static Pattern bbCodeUrlPattern = Pattern.compile("\\[url=(.+)\\]\\1\\[\\/url\\]");
    private static HashMap<String, String> rules = new HashMap<String, String>() {{
        put("\\[b\\]", "");
        put("\\[/b\\]", "");
        put("\\[url\\]", "");
        put("\\[/url\\]", "");
    }};

    @Override
    public void handleMessage(Message message) {
        Matcher m = bbCodeUrlPattern.matcher(message.getText());
        int position = 0;
        while (m.find(position)) {
            position = m.start();
            message.setText(m.replaceFirst(m.group(1)));
            m = bbCodeUrlPattern.matcher(message.getText());
        }

        String messageText = message.getText();
        Set<String> set = rules.keySet();
        for (String pattern : set) {
            messageText = messageText.replaceAll(pattern, rules.get(pattern));
        }
        message.setText(messageText);
    }
}
