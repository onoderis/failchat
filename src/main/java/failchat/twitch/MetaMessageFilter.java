package failchat.twitch;

import failchat.core.Message;
import failchat.core.MessageFilter;

import java.util.logging.Logger;

/**
 * Ипользуется для обработки и дальнейшей фильтрации мета-сообщений от jtv.
 * По мета-сообщению можно пределить является ли пользователь подписчиком, турбо-пользователем,
 * его цвет ника (сообщения), доступные сеты смайлов.
 */
public class MetaMessageFilter implements MessageFilter<TwitchMessage> {
    private static final Logger logger = Logger.getLogger(MetaMessageFilter.class.getName());
    private static final String TWITCH_INFO_USER = "jtv";
    private static final String USER_COLOR_META = "USERCOLOR";
    private static final String EMOTICONS_META = "EMOTESET";

    private String user = null;

    @Override
    public boolean filterMessage(TwitchMessage message) {
        // проверка является ли мета сообщением
        if (!TWITCH_INFO_USER.equals(message.getAuthor())) {
            user = null;
            return true;
        }

        String[] s = message.getText().split(" ");
        if (user == null) { //is first meta message?
            user = s[1];
        }
        else {
            if (!user.equals(s[1])) {
                logger.warning("Wrong order of meta messages. Was: " + user + "; is: " + s[1]);
                return false; // сомнительно, посмотреть что будет
            }
        }

        switch (s[0]) {
            case EMOTICONS_META: { //message format: EMOTESET nickname [0,16,362,888]
                message.setEmoteSets(parseEmoteSets(s[2]));
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
