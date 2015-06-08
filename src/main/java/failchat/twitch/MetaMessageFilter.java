package failchat.twitch;

import failchat.core.Message;
import failchat.core.MessageFilter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Ипользуется для обработки и дальнейшей фильтрации мета-сообщений от jtv.
 * По мета-сообщениям можно пределить является ли пользователь подписчиком, турбо-пользователем,
 * его цвет ника (сообщения), доступные сеты смайлов.
 * Информация из мета-сообщений хранится в MetaProperties и вливается в сообщение когда оно приходит.
 */
public class MetaMessageFilter implements MessageFilter<TwitchMessage> {
    private static final Logger logger = Logger.getLogger(MetaMessageFilter.class.getName());
    private static final String TWITCH_INFO_USER = "jtv";
    private static final String USER_COLOR_META = "USERCOLOR";
    private static final String EMOTICONS_META = "EMOTESET";

    private Map<String, MetaProperties> propertiesMap = new HashMap<>();

    @Override
    public boolean filterMessage(TwitchMessage message) {
        // проверка является ли мета сообщением
        if (!TWITCH_INFO_USER.equals(message.getAuthor())) {
            MetaProperties properties = propertiesMap.get(message.getAuthor());
            if (properties != null) {
                message.setProperties(properties);
                propertiesMap.remove(message.getAuthor());
            }
            return true;
        }

        String[] s = message.getText().split(" ");
        String user = s[1];
        MetaProperties properties = propertiesMap.get(user);
        if (properties == null) {
            properties = new MetaProperties();
            propertiesMap.put(user, properties);
        }
        switch (s[0]) {
            case EMOTICONS_META: { //message format: EMOTESET nickname [0,16,362,888]
                properties.setEmoteSets(parseEmoteSets(s[2]));
                break;
            }
        }

        return false;
    }

    private int[] parseEmoteSets(String sets) {
        String[] setMasStr = sets.replace("[", "").replace("]", "").split(",");
        int[] setMas = new int[setMasStr.length];
        for (int i = 0; i < setMasStr.length; i++) {
            setMas[i] = Integer.parseInt(setMasStr[i]);
        }
        return setMas;
    }
}
