package failchat.twitch;

import failchat.core.MessageHandler;

/**
 * Заменяет символы '<' и '>' на character entities
 * */
public class HtmlHandler implements MessageHandler<TwitchMessage> {
    @Override
    public void handleMessage(TwitchMessage message) {
        String messageText = message.getText();
        messageText = messageText.replaceAll("<", "&lt;");
        messageText = messageText.replaceAll(">", "&gt;");
        message.setText(messageText);
    }
}
