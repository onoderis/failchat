package failchat.core;

import failchat.goodgame.GoodgameChatClient;
import failchat.sc2tv.Sc2tvChatClient;
import failchat.twitch.TwitchChatClient;

import java.util.ArrayList;
import java.util.List;

public class Configurator {

    private static int exampleId = 0;
//    private static int exampleId = 160916; //abver
//    private static int exampleId = 157064; //dave

    private static String twitchTestChannel = "forsenlol";
//    private static String twitchTestChannel = "trumpsc";
//    private static int ggChannel = 20296;
    private static String ggChannel = "fail0001";

    private MessageManager messageManager;
    private List<ChatClient> chatClients = new ArrayList<>();


    public Configurator(MessageManager mm) {
        messageManager = mm;
    }

    public void initializeChatClients() {
        ChatClient sc2tvChatClient = new Sc2tvChatClient(exampleId, messageManager.getMessagesQueue());
        chatClients.add(sc2tvChatClient);

        TwitchChatClient twitchChatClient = new TwitchChatClient(twitchTestChannel, messageManager.getMessagesQueue());
        chatClients.add(twitchChatClient);

        GoodgameChatClient ggcc = new GoodgameChatClient(ggChannel, messageManager.getMessagesQueue());
        chatClients.add(ggcc);

//        TestChatClient tcc = new TestChatClient(messageManager.getMessagesQueue());
//        chatClients.add(tcc);

        for (ChatClient cc : chatClients) {
            cc.goOnline();
        }
    }

    public void turnOffChatClients() {
        for (ChatClient cc : chatClients) {
            cc.goOffline();
        }
    }



}
