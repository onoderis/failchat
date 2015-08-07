package failchat.handlers;

import failchat.core.Message;
import failchat.core.MessageHandler;

import java.util.logging.Logger;
/**
 * Заменяет символы { и } на html entity
 * */
public class MessageObjectCleaner implements MessageHandler {
    private static volatile MessageObjectCleaner instance;
    private static final Logger logger = Logger.getLogger(MessageObjectCleaner.class.getName());

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

    @Override
    public void handleMessage(Message message) {
        String mes = message.getText();
        mes = mes.replaceAll("\\{", "&#123;");
        mes = mes.replaceAll("}", "&#125;");
        message.setText(mes);
    }
}