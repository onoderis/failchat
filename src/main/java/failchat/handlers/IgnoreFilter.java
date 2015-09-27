package failchat.handlers;

import failchat.core.Configurator;
import failchat.core.Message;
import failchat.core.MessageFilter;
import failchat.core.Source;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Фильтрует сообщения от пользователей в игнор-листе
 * Баны хранятся в формате nickname#source
 * */
public class IgnoreFilter implements MessageFilter {
    private static final Logger logger = Logger.getLogger(IgnoreFilter.class.getName());
    private Set<String> ignoreSet = new HashSet<>();
    private Pattern banFormat;

    public IgnoreFilter() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (Source source : Source.values()) {
            sb.append(source.getLowerCased()).append('|');
        }
        sb.deleteCharAt(sb.length() - 1).append(')');
        banFormat = Pattern.compile(".+#" + sb.toString());

        reloadIgnoreList();
    }

    @Override
    public boolean filterMessage(Message message) {
        return ignoreSet.contains(message.getAuthor() + "#" + message.getSource().getLowerCased());
    }

    public void ignore(String user) {
        ignoreSet.add(user);
        Configurator.config.setProperty("ignore", ignoreSet.toArray());
        logger.fine("User ignored: " + user);
    }

    public void reloadIgnoreList() {
        ignoreSet.clear();
        Configurator.config.getList("ignore").forEach((bannedUser) -> {
            if (banFormat.matcher((String) bannedUser).find())
                ignoreSet.add((String) bannedUser);
        });
    }
}
