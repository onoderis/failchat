package failchat.handlers;

import failchat.core.Configurator;
import failchat.core.Message;
import failchat.core.MessageFilter;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Фильтрует сообщения от пользователей в игнор-листе
 * Баны хранятся в формате nickname#source
 * */
public class IgnoreFilter implements MessageFilter {
    private static final Logger logger = Logger.getLogger(IgnoreFilter.class.getName());
    private Set<String> ignoreSet = new HashSet<>();

    public IgnoreFilter() {
        Configurator.config.getList("ignore").forEach((bannedUser) -> ignoreSet.add((String)bannedUser));
    }

    @Override
    public boolean filterMessage(Message message) {
        return ignoreSet.contains(message.getAuthor() + "#" + message.getSource().getLowerCased());
    }

    public void ignore(String user) {
        ignoreSet.add(user);
        logger.fine("User ignored: " + user);
    }

    public void saveIgnoreList() {
        Configurator.config.setProperty("ignore", ignoreSet.toArray());
    }
}
