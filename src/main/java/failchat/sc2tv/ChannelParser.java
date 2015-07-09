package failchat.sc2tv;


import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChannelParser {
    private static final Logger logger = Logger.getLogger(ChannelParser.class.getName());
    private static final String channelUrl = "http://sc2tv.ru/channel/";
    //pattern for chat iframe
    private static final Pattern channelIdPattern = Pattern.compile("src=\"http:\\/\\/chat.sc2tv.ru\\/index.htm\\?channelId=(\\d*)&");
    private static final String[] charsToDelete = {"_", "\\.", "\\[", "\\]"};

    public static int getChannelId(String channel) {
        try {
            for (String s: charsToDelete) {
                channel = channel.replaceAll(s, "");
            }
            channel = channel.replace(' ', '-');
            URLConnection connection = new URL(channelUrl + channel).openConnection();
            connection.setRequestProperty("User-Agent", "failchat client");
            String rawHtml = IOUtils.toString(connection.getInputStream());
            Matcher m = channelIdPattern.matcher(rawHtml);
            if (!m.find()) {
                logger.severe("Can't find sc2tv channel name");
                return -1;
            }
            return Integer.parseInt(m.group(1));
        } catch (IOException e) {
            logger.severe("Can't load/parse sc2tv channel page");
            e.printStackTrace();
            return -1;
        }
    }
}
