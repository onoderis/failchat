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
    private static String announcePattern = "\\[url=.+?\\](.+?)\\[\\/url\\]";
    private static HashMap<Pattern, String> rules = new HashMap<Pattern, String>() {{
        put(Pattern.compile("\\[url=(.+)\\]\\1\\[\\/url\\]"), "\1"); //иногда ссылки приходят в виде [url=http://xxx.x]http://xxx.x[/url]
        put(Pattern.compile("\\[b\\]"), "");
        put(Pattern.compile("\\[/b\\]"), "");
        put(Pattern.compile("\\[url\\]"), "");
        put(Pattern.compile("\\[/url\\]"), "");
    }};

    @Override
    public void handleMessage(Message message) {
        //announce or donation
        if (message.getAuthor().equals("SC2TV")) {
            message.setText(message.getText().replaceAll(announcePattern, "\1"));
        }

        //rules processing
        String messageText = message.getText();
        Set<Pattern> patternSet = rules.keySet();
        for (Pattern pattern : patternSet) {
            Matcher m = pattern.matcher(messageText);
            if (m.find()) {
                messageText = m.replaceAll(rules.get(pattern));
            }
        }
        message.setText(messageText);
    }
}
