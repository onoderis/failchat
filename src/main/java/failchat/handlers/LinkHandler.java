package failchat.handlers;

import failchat.core.Image;
import failchat.core.Link;
import failchat.core.Message;
import failchat.core.MessageHandler;
import org.apache.commons.lang.StringUtils;

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

    private static String[] IMG_FORMATS = {".jpg", ".jpeg", ".png", ".gif"};
    private boolean showImages = false;

    @Override
    public void handleMessage(Message message) {
        Matcher m = URL_PATTERN.matcher(message.getText());
        while (m.find()) {
            String objectNumber;
            if (showImages && StringUtils.endsWithAny(m.group(), IMG_FORMATS)) {
                Image image = new Image(m.group());
                objectNumber = message.addImage(image);
            } else {
                Link url = new Link(m.group(), m.group(4), m.group(3));
                objectNumber = message.addLink(url);
            }
            message.setText(m.replaceFirst(objectNumber));
            m = URL_PATTERN.matcher(message.getText());
        }
    }

    public void setShowImages(boolean show) {
        showImages = show;
    }
}
