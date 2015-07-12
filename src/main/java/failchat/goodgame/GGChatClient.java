package failchat.goodgame;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.*;
import org.apache.commons.io.IOUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
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
    private static final int RECONNECT_TIMEOUT = 5000;
    private static final String NEW_MESSAGE_SEQUENCE = "\"type\":\"message\"";

    private WebSocketClient wsClient;
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
        messageHandlers.add(new GGHighlightHandler(channelName));
        status = ChatClientStatus.READY;
    }

    @Override
    public void goOnline() {
        if (status != ChatClientStatus.READY) {
            return;
        }
        channelId = getChannelIdByName(channelName);
        tryToConnect();
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

    private void tryToConnect() {
        while (status == ChatClientStatus.CONNECTING || status == ChatClientStatus.READY) {
            wsClient = new GGWSClient();
            try {
                if (status == ChatClientStatus.CONNECTING) {
                    Thread.sleep(RECONNECT_TIMEOUT);
                }
                if (!wsClient.connectBlocking()) {
                    continue;
                }
                String connectToChannelMes = objectMapper.writeValueAsString(new JoinWSMessage((channelId)));
                wsClient.send(connectToChannelMes);
                status = ChatClientStatus.WORKING;
            } catch (JsonProcessingException e) {
                logger.severe("Goodgame bad json");
                status = ChatClientStatus.ERROR;
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class GGWSClient extends WebSocketClient {
        GGWSClient() {
            super(URI.create(GG_WS_URL));
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            logger.info("Connected to goodgame");
            messageManager.sendInfoMessage(new InfoMessage(Source.GOODGAME, "connected"));
        }

        @Override
        public void onMessage(String s) {
            if (s.contains(NEW_MESSAGE_SEQUENCE)) {
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
        public void onClose(int i, String s, boolean b) { // вызывается даже если веб сокет не был подключён
            if (status == ChatClientStatus.CONNECTING || status == ChatClientStatus.ERROR || status == ChatClientStatus.SHUTDOWN) {
                return;
            }
            if (status == ChatClientStatus.WORKING) {
                status = ChatClientStatus.CONNECTING;
            }
            logger.info("Goodgame disconnected");
            messageManager.sendInfoMessage(new InfoMessage(Source.GOODGAME, "disconnected"));
            tryToConnect();
        }

        @Override
        public void onError(Exception e) {
            logger.warning("Goodgame web socket error");
            e.printStackTrace();
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