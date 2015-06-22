package failchat.sc2tv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class Sc2tvChatClient implements ChatClient, Runnable {
    private static final Logger logger = Logger.getLogger(Sc2tvChatClient.class.getName());
    private static final String CHAT_URL = "http://chat.SC2TV.ru/memfs/channel-";
    private static final String CHAT_URL_END = ".json";
    private static final String THREAD_NAME= "Sc2tvChatClient";
    private static final SimpleDateFormat HEADER_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH); //example: "Sat, 11 Apr 2015 23:23:43 GMT"
    private static final SimpleDateFormat JSON_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //example: "2015-04-12 02:03:06"
    private static final long TIMEOUT = 5000;

    private final Queue<Message> messageQueue = MessageManager.getInstance().getMessagesQueue();
    private List<MessageHandler> messageHandlers;
    private String channelName;
    private int channelId = -1;
    private long lastMessageTime = System.currentTimeMillis();
    private URL chatUrl;
    private boolean exitFlag = false;
    private long lastModified = System.currentTimeMillis();
    private ObjectMapper objectMapper;
    private long requestTime = 0;
    private ChatClientStatus status;

    public Sc2tvChatClient(String channelName) {
        this.channelName = channelName;
        objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(JSON_DATE_FORMAT);
        messageHandlers = new ArrayList<>();
        messageHandlers.add(MessageObjectCleaner.getInstance());
        messageHandlers.add(new BBCodeHandler());
        messageHandlers.add(new Sc2tvSmileHandler());
        status = ChatClientStatus.READY;
    }

    @Override
    public void goOffline() {
        exitFlag = true;
    }

    @Override
    public void goOnline() {
        if (status != ChatClientStatus.READY) {
            return;
        }
        channelId  = ChannelParser.getChannelId(channelName);
        if (channelId == -1) {
            return;
        }
        try {
            chatUrl = new URL(CHAT_URL + channelId + CHAT_URL_END);
        } catch (MalformedURLException e) {
            logger.severe("Bad sc2tv channel id: " + channelId);
            e.printStackTrace();
        }

        Thread t = new Thread(this);
        t.setName(THREAD_NAME);
        t.start();
    }

    @Override
    public ChatClientStatus getStatus() {
        return status;
    }

    @Override
    public void run() {
        lastModified = System.currentTimeMillis();
        while (!exitFlag) {
            makeIteration();
            try {
                synchronized (this) {
                    if (TIMEOUT > requestTime) {
                        wait(TIMEOUT - requestTime);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        status = ChatClientStatus.READY;
    }

    private void makeIteration() {
        try {
            long t = System.currentTimeMillis();
            HttpURLConnection urlCon = (HttpURLConnection)chatUrl.openConnection();
            urlCon.setIfModifiedSince(lastModified);
            urlCon.setRequestProperty("User-Agent", "failchat client");
            if (urlCon.getResponseCode() != 200) {
                requestTime = 0;
                return;
            }
            requestTime = System.currentTimeMillis() - t;

            lastModified = urlCon.getLastModified();

            HashMap<String, List<Sc2tvMessage>> mHash = objectMapper.readValue(urlCon.getInputStream(), new TypeReference<HashMap<String, List<Sc2tvMessage>>>(){});
            List<Sc2tvMessage> messages = mHash.get("messages");
            int lastMessageIndex = -1;
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).getTimestamp().getTime() > lastMessageTime) {
                    lastMessageIndex = i;
                } else {
                    break;
                }
            }
            for (int i = lastMessageIndex; i >= 0; i--) {
                Message m = messages.get(i);
                for (MessageHandler mh : messageHandlers) {
                    mh.handleMessage(m);
                }
                messageQueue.add(m);
            }
            lastMessageTime = messages.get(0).getTimestamp().getTime();

            status = ChatClientStatus.WORKING;

            synchronized (messageQueue) {
                messageQueue.notify();
            }
        } catch (JsonProcessingException e) {
            status = ChatClientStatus.ERROR;
            exitFlag = true;
        }
        catch (IOException e) {
            requestTime = 0;
            status = ChatClientStatus.CONNECTING;
        }
    }
}
