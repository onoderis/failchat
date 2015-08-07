package failchat.handlers;

import failchat.core.Message;
import failchat.core.MessageHandler;

/**
 * Заменяет символы '<' и '>' на character entities
 * */
public class HtmlHandler implements MessageHandler {
    @Override
    public void handleMessage(Message message) {
        String messageText = message.getText();
        messageText = messageText.replaceAll("<", "&lt;");
        messageText = messageText.replaceAll(">", "&gt;");
        message.setText(messageText);
    }
}
