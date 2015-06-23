package failchat.twitch;

import com.sorcix.sirc.*;
import failchat.core.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

public class TwitchChatClient implements ChatClient {

    private static final Logger logger = Logger.getLogger(TwitchChatClient.class.getName());
    private static final String TWITCH_IRC_URL = "irc.twitch.tv";
    private static final int TWITCH_IRC_PORT = 6667;
    private static final String BOT_NAME = "fail_chatbot";
    private static final String BOT_PASSWORD = "oauth:59tune21e6ymz4xg57snr77tcsbg2y";
    private static final int RECONNECT_TIMEOUT = 5000;

    private final Queue<Message> messageQueue = MessageManager.getInstance().getMessagesQueue();
    private IrcConnection ircConnection;
    private String channelName;
    private List<MessageHandler<TwitchMessage>> messageHandlers;
    private List<MessageFilter<TwitchMessage>> messageFilters;
    private ChatClientStatus status;

    public TwitchChatClient(String channelName) {
        this.channelName = channelName;
        messageFilters = new ArrayList<>();
        messageFilters.add(new MetaMessageFilter());
        messageHandlers = new ArrayList<>();
        messageHandlers.add(MessageObjectCleaner.getInstance());
        messageHandlers.add(new TwitchSmileHandler());
        messageHandlers.add(new TwitchHighlightHandler(channelName));
        status = ChatClientStatus.READY;
    }

    @Override
    public void goOnline() {
        if (status != ChatClientStatus.READY) {
            return;
        }
        ircConnection = new IrcConnection(TWITCH_IRC_URL, TWITCH_IRC_PORT, BOT_PASSWORD);
        ircConnection.setNick(BOT_NAME);
        ircConnection.addMessageListener(new MyIrcAdapter());
        try {
            ircConnection.connect();
        } catch (IOException e) {
            e.printStackTrace();
            status = ChatClientStatus.CONNECTING;
            return;
        } catch (NickNameException | PasswordException e) {
            e.printStackTrace();
            status = ChatClientStatus.ERROR;
            return;
        }
        logger.info("Connected to TWITCH IRC server");
        ircConnection.createChannel(channelName.toLowerCase()).join(); // в irc каналы создаются в lower case
        logger.info("Connected to irc channel: " + channelName);
        ircConnection.sendRaw("TWITCHCLIENT 3"); // чтобы слались мета-сообщения
    }

    @Override
    public void goOffline() {
        status = ChatClientStatus.SHUTDOWN;
        ircConnection.disconnect();
    }

    @Override
    public ChatClientStatus getStatus() {
        return status;
    }

    private class MyIrcAdapter extends IrcAdaptor {
        public void onMessage(IrcConnection irc, User sender, Channel target, String message) {
//                logger.fine(message);
            TwitchMessage m = new TwitchMessage(sender.getNick(), message);
            for (MessageFilter<TwitchMessage> mf : messageFilters) {
                if (!mf.filterMessage(m)) {
                    return;
                }
            }
            for (MessageHandler<TwitchMessage> mh : messageHandlers) {
                mh.handleMessage(m);
            }
            messageQueue.add(m);
            synchronized (messageQueue) {
                messageQueue.notify();
            }
        }

        @Override
        public void onDisconnect(IrcConnection irc) {
            if (status == ChatClientStatus.READY) {
                status = ChatClientStatus.CONNECTING;
            }
            logger.info("Twitch disconnected");
        }
    }
}

