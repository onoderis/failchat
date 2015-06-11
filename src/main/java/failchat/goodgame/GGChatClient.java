package failchat.goodgame;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.ChatClient;
import failchat.core.ChatClientStatus;
import failchat.core.Message;
import failchat.core.MessageHandler;
import org.apache.commons.io.IOUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GGChatClient extends WebSocketClient implements ChatClient {

    private static final String GG_WS_URL = "ws://chat.goodgame.ru:8081/chat/websocket";
    private static final String GG_STREAM_API_URL = "http://goodgame.ru/api/getchannelstatus?fmt=json&id=";
    private static final String EXTRACT_CHANNEL_ID_REGEX = "\"stream_id\":\"(\\d*)\"";
    private static final int RECONNECT_TIMEOUT = 5000;
    private static final String NEWMESSAGE_SEQUENCE = "\"type\":\"message\"";

    private ChatClientStatus status = ChatClientStatus.READY;
    private Queue<Message> messageQueue;
    private List<MessageHandler<GGMessage>> messageHandlers;
    private String channelName;
    private int channelId;
    ObjectMapper objectMapper;

    public GGChatClient(String channelName, Queue<Message> mq) {
        super(URI.create(GG_WS_URL));
        this.channelName = channelName;
        messageQueue = mq;
        objectMapper = new ObjectMapper();
        messageHandlers = new ArrayList<>();
        messageHandlers.add(new UrlCleaner());
        messageHandlers.add(new GGSmileHandler());
    }

    @Override
    public void goOnline() {
        if (status != ChatClientStatus.READY) {
            return;
        }

        channelId = getChannelIdByName(channelName);

        try {
            connectBlocking();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            String connectToChannelMes = objectMapper.writeValueAsString(new JoinWSMessage((channelId)));
            send(connectToChannelMes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void goOffline() {
        close();
        status = ChatClientStatus.READY;
    }

    @Override
    public ChatClientStatus getStatus() {
        return status;
    }


    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    @Override
    public void onMessage(String s) {
        if (s.contains(NEWMESSAGE_SEQUENCE)) {
            try {
                GoodgameWSMessage ggwsm = objectMapper.readValue(s, new TypeReference<GoodgameWSMessage>() {});
                GGMessage message = ggwsm.getMessage();
                //handling messages
                for (MessageHandler<GGMessage> messageHandler : messageHandlers) {
                    messageHandler.handleMessage(message);
                }
                messageQueue.add(message);
                synchronized (messageQueue) {
                    messageQueue.notify();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {

    }

    @Override
    public void onError(Exception e) {

    }

    private int getChannelIdByName(String name) {
        try {
            URL url = new URL(GG_STREAM_API_URL + name);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() != 200) {
                return -1;
            }
            String response = IOUtils.toString(connection.getInputStream());
            Pattern p = Pattern.compile(EXTRACT_CHANNEL_ID_REGEX);
            Matcher m = p.matcher(response);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            status = ChatClientStatus.ERROR;
        } catch (IOException e) {
            e.printStackTrace();
            status = ChatClientStatus.CONNECTING;
        }
        return -1;
    }

    /**
     * Класс для сериализации в json. Используется для подключения к каналу.
     */
    static class JoinWSMessage {
        private String type = "join";
        private JoinWSData data;

        public String getType() {
            return type;
        }

        public JoinWSData getData() {
            return data;
        }

        JoinWSMessage(int channelId) {
            data = new JoinWSData(channelId);
        }
    }

    /**
     * Класс для сериализации в json. Используется для подключения к каналу.
     */
    static class JoinWSData {
        private String channelId;

        private boolean hidden = false;

        JoinWSData(int channelId) {
            this.channelId = Integer.toString(channelId);
        }

        @JsonProperty("channel_id")
        public String getChannelId() {
            return channelId;
        }

        public boolean isHidden() {
            return hidden;
        }
    }

    /**
     * Класс для десериализации из json'a. Представляет сообщение из чата
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GoodgameWSMessage {
        protected String type;
        protected GGMessage message;

        public GGMessage getMessage() {
            return message;
        }

        @JsonProperty("data")
        public void setMessage(GGMessage message) {
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}