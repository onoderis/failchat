package failchat.core;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageObjectCleaner implements MessageHandler {

    public static final Pattern pattern = Pattern.compile("\\{!\\d{1,3}\\}");
    private static volatile MessageObjectCleaner instance;


    private MessageObjectCleaner() {

    }

    public static MessageObjectCleaner getInstance() {
        MessageObjectCleaner localInstance = instance;
        if (localInstance == null) {
            synchronized (MessageObjectCleaner.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new MessageObjectCleaner();
                }
            }
        }
        return localInstance;
    }

    private static final Logger logger = Logger.getLogger(MessageObjectCleaner.class.getName());

    @Override
    public void handleMessage(Message message) {
        Matcher m = pattern.matcher(message.getText());
        if (m.find()) {
            logger.fine("Message cleaned - " + message.getAuthor() + ": " + message.getText());
            message.setText(m.replaceAll(""));
        }
    }
}