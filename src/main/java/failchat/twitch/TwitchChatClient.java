package failchat.twitch;

import com.sorcix.sirc.*;
import failchat.core.ChatClient;
import failchat.core.ChatClientStatus;
import failchat.core.Logging;
import failchat.core.Message;

import java.io.IOException;
import java.util.Queue;
import java.util.logging.Logger;

public class TwitchChatClient implements ChatClient {

    private static final Logger logger = Logger.getLogger(TwitchChatClient.class.getName());
    private static final String TWITCH_IRC_URL = "irc.twitch.tv";
    private static final int TWITCH_IRC_PORT = 6667;
    private static final String BOT_NAME = "fail_chatbot";
    private static final String BOT_PASSWORD = "oauth:59tune21e6ymz4xg57snr77tcsbg2y";
    private static final int RECONNECT_TIMEOUT = 5000;

    private IrcConnection ircConnection;
    private String channelName;
    private ChatClientStatus status = ChatClientStatus.READY;
    private final Queue<Message> messageQueue;

    public TwitchChatClient(String channelName, Queue<Message> mq) {
        this.channelName = channelName;
        messageQueue = mq;
    }

    @Override
    public void goOnline() {
        if (status != ChatClientStatus.READY) {
            return;
        }
        ircConnection = new IrcConnection(TWITCH_IRC_URL, TWITCH_IRC_PORT, BOT_PASSWORD);
        ircConnection.setNick(BOT_NAME);

        ircConnection.addMessageListener(new IrcAdaptor() {
            @Override
            public void onMessage(IrcConnection irc, User sender, Channel target, String message) {
                Message m = new TwitchMessage(sender.getNick(), message);
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
                try {
                    Thread.currentThread().sleep(RECONNECT_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

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
        ircConnection.createChannel(channelName).join();
    }

    @Override
    public void goOffline() {
        ircConnection.disconnect();
    }

    @Override
    public ChatClientStatus getStatus() {
        return status;
    }

}

