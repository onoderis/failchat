package failchat.goodgame;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.*;
import failchat.handlers.CommonHighlightHandler;
import failchat.handlers.MessageObjectCleaner;
import org.apache.commons.io.IOUtils;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GGChatClient implements ChatClient {
    private static final Logger logger = Logger.getLogger(GGChatClient.class.getName());
    private static final String GG_WS_URL = "ws://chat.goodgame.ru:8081/chat/websocket";
    private static final String GG_STREAM_API_URL = "http://goodgame.ru/api/getchannelstatus?fmt=json&id=";
    private static final Pattern EXTRACT_CHANNEL_ID_REGEX = Pattern.compile("\"stream_id\":\"(\\d*)\"");
    private static final String NEW_MESSAGE_SEQUENCE = "\"type\":\"message\"";

    private WSClient wsClient;
    private ChatClientStatus status;
    private MessageManager messageManager = MessageManager.getInstance();
    private Queue<Message> messageQueue = messageManager.getMessagesQueue();
    private List<MessageHandler<GGMessage>> messageHandlers;
    private String channelName;
    private int channelId;
    private ObjectMapper objectMapper;

    public GGChatClient(String channelName) {
        this.channelName = channelName;
        objectMapper = new ObjectMapper();
        messageHandlers = new ArrayList<>();
        //noinspection unchecked
        messageHandlers.add(MessageObjectCleaner.getInstance());
        messageHandlers.add(new UrlCleaner());
        messageHandlers.add(new GGSmileHandler());
        messageHandlers.add(new CommonHighlightHandler(channelName));
        status = ChatClientStatus.READY;
    }

    @Override
    public void goOnline() {
        if (status != ChatClientStatus.READY) {
            return;
        }
        channelId = getChannelIdByName(channelName);
        wsClient = new GGWSClient();
        wsClient.connect();
    }

    @Override
    public void goOffline() {
        status = ChatClientStatus.SHUTDOWN;
        if (wsClient != null) {
            wsClient.close();
        }
    }

    @Override
    public ChatClientStatus getStatus() {
        return status;
    }

    private int getChannelIdByName(String name) {
        try {
            URL url = new URL(GG_STREAM_API_URL + name);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() != 200) {
                return -1;
            }
            String response = IOUtils.toString(connection.getInputStream());
            Matcher m = EXTRACT_CHANNEL_ID_REGEX.matcher(response);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (IOException e) {
            e.printStackTrace();
            status = ChatClientStatus.ERROR;
        }
        return -1;
    }

    private class GGWSClient extends WSClient {
        GGWSClient() {
            super(GG_WS_URL);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            try {
                String connectToChannelMes = objectMapper.writeValueAsString(new JoinWSMessage((channelId)));
                wsClient.send(connectToChannelMes);
                status = ChatClientStatus.WORKING;
                logger.info("Connected to goodgame");
                messageManager.sendInfoMessage(new InfoMessage(Source.GOODGAME, "connected"));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMessage(String s) {
            if (s.contains(NEW_MESSAGE_SEQUENCE)) {
                try {
                    GoodgameWSMessage ggwsm = objectMapper.readValue(s, GoodgameWSMessage.class);
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
            logger.info("Goodgame disconnected");
        }

        @Override
        public void onReconnect() {
            logger.info("Goodgame disconnected, trying to reconnect ...");
            messageManager.sendInfoMessage(new InfoMessage(Source.GOODGAME, "disconnected"));
        }
    }


    /**
     * Класс для сериализации в json. Используется для подключения к каналу.
     */
    private static class JoinWSMessage {
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
    private static class JoinWSData {
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