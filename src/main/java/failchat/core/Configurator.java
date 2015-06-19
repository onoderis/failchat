package failchat.core;

import failchat.goodgame.GGChatClient;
import failchat.sc2tv.Sc2tvChatClient;
import failchat.twitch.TwitchChatClient;

import java.util.HashMap;
import java.util.Map;

public class Configurator {

    private static String sc2tvTestChannel = "fail0001";
    private static String twitchTestChannel = "fail0001";
//    private static String twitchTestChannel = "forsenlol";
    private static String ggChannel = "fail0001";

    private MessageManager messageManager;
    private Map<Source, ChatClient> chatClients = new HashMap<>();

    public Configurator(MessageManager mm) {
        messageManager = mm;
    }

    public void initializeChatClients() {
        ChatClient sc2tvChatClient = new Sc2tvChatClient(sc2tvTestChannel, messageManager.getMessagesQueue());
        chatClients.put(Source.SC2TV, sc2tvChatClient);

        GGChatClient ggcc = new GGChatClient(ggChannel, messageManager.getMessagesQueue());
        chatClients.put(Source.GOODGAME, ggcc);

        TwitchChatClient twitchChatClient = new TwitchChatClient(twitchTestChannel, messageManager.getMessagesQueue());
        chatClients.put(Source.TWITCH, twitchChatClient);

//        TestChatClient tcc = new TestChatClient(messageManager.getMessagesQueue());
//        chatClients.put(Source.TEST, tcc);

        chatClients.values().forEach(failchat.core.ChatClient::goOnline);
    }

    public void turnOffChatClients() {
        chatClients.values().forEach(failchat.core.ChatClient::goOffline);
    }



}
