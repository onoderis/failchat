package failchat.core;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlHandler implements MessageHandler {
    private static final Logger logger = Logger.getLogger(UrlHandler.class.getName());

    /**
     * Unescaped regex: \b(https?|ftps?):\/\/(w{3}\.)?(([-\w\d+&@#%?=~_|!:,.;]+)[\/\S]*)
     * Capture groups:
     * 1: protocol (http, https, ftp, ftps)
     * 2: www.
     * 3: short url
     * 4: domain
     */
    private static Pattern URL_PATTERN = Pattern.compile("\\b(https?|ftps?):\\/\\/(w{3}\\.)?(([-\\w\\d+&@#%?=~_|!:,.;]+)[\\/\\S]*)");

    private  Url[] buffer = new Url[60];

    @Override
    public void handleMessage(Message message) {
        Matcher m = URL_PATTERN.matcher(message.getText());
        while (m.find()) {
//            logger.fine("found url: " + m.group(3));
            Url url = new Url(m.group(), m.group(4), m.group(3));
            message.setText(m.replaceFirst(message.addLink(url)));
            m = URL_PATTERN.matcher(message.getText());
        }
    }


}
