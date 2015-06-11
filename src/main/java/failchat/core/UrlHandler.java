package failchat.core;

import java.util.Arrays;
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
        int urlCount = 0;
        int position = 0;
        while (m.find(position)) {
            logger.fine("found url: " + m.group(3));
            urlCount++;
            position = m.start();
            Url url = new Url(position, m.group(), m.group(4), m.group(3));
            buffer[urlCount - 1] = url;
            message.setText(m.replaceFirst(""));
            m = URL_PATTERN.matcher(message.getText());
        }
        if (urlCount > 0) {
            message.setLinks(Arrays.copyOf(buffer, urlCount));
        }
    }


}
