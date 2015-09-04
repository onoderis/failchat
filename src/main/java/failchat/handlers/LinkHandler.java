package failchat.handlers;

import failchat.core.Link;
import failchat.core.Message;
import failchat.core.MessageHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkHandler implements MessageHandler {
    /**
     * Unescaped regex: \b(https?|ftps?):\/\/(w{3}\.)?(([-\w\d+&@#%?=~_|!:,.;]+)[\/\S]*)
     * Capture groups:
     * 1: protocol (http, https, ftp, ftps)
     * 2: www.
     * 3: short url
     * 4: domain
     */
    public static Pattern URL_PATTERN = Pattern.compile("\\b(https?|ftps?):\\/\\/(w{3}\\.)?(([-\\w\\d+&@#%?=~_|!:,.;]+)[\\/\\S]*)");

    @Override
    public void handleMessage(Message message) {
        Matcher m = URL_PATTERN.matcher(message.getText());
        while (m.find()) {
            Link url = new Link(m.group(), m.group(4), m.group(3));
            message.setText(m.replaceFirst(message.addLink(url)));
            m = URL_PATTERN.matcher(message.getText());
        }
    }


}
