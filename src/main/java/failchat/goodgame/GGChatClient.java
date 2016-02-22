package failchat.goodgame;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.*;
import failchat.handlers.CommonHighlightHandler;
import failchat.handlers.MessageObjectCleaner;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GGChatClient implements ChatClient {

    private static final Logger logger = Logger.getLogger(GGChatClient.class.getName());
    private static final String GG_WS_URL = "ws://chat.goodgame.ru:8081/chat/websocket";

    private WSClient wsClient;
    private ChatClientStatus status;
    private MessageManager messageManager = MessageManager.getInstance();
    private Moderation moderation = Moderation.getInstance();
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
        channelId = GGApiWorker.getChannelIdByName(channelName);
        if (channelId == -1) {
            status = ChatClientStatus.ERROR;
            return;
        }
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
                logger.log(Level.WARNING, "Something goes wrong...", e);
            }
        }

        @Override
        public void onMessage(String s) {
            try {
                GoodgameCommonMessage ggwsm = objectMapper.readValue(s, GoodgameCommonMessage.class);
                switch (ggwsm.getType()) {
                    case "message": {
                        handleUserMessage(ggwsm.getData());
                        break;
                    }
                    case "remove_message": {
                        handleModMessage(ggwsm.getData());
                        break;
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Something goes wrong...", e);
            }
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            logger.info("Goodgame disconnected");
        }

        @Override
        public void onError(Exception e) {
            logger.log(Level.WARNING, "Disconnect? ...", e);
        }

        @Override
        public void onReconnect() {
            logger.info("Goodgame disconnected, trying to reconnect ...");
            messageManager.sendInfoMessage(new InfoMessage(Source.GOODGAME, "disconnected"));
        }

        private void handleUserMessage(JsonNode messageObj) throws IOException {
            GGMessage message = objectMapper.convertValue(messageObj, GGMessage.class);
            for (MessageHandler<GGMessage> messageHandler : messageHandlers) {
                messageHandler.handleMessage(message);
            }
            messageManager.sendMessage(message);
        }

        private void handleModMessage(JsonNode messageObj) throws IOException {
            GoodgameDeleteMessage ggDelMes = objectMapper.convertValue(messageObj, GoodgameDeleteMessage.class);
            moderation.deleteGgMessage(ggDelMes.getMessageId());
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
     * Класс для десериализации из json'a. Представляет общий вид сообщения
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GoodgameCommonMessage {
        private String type;
        private JsonNode data;

        public JsonNode getData() {
            return data;
        }

        public void setData(JsonNode data) {
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GoodgameDeleteMessage {
        protected int messageId;

        public int getMessageId() {
            return messageId;
        }

        @JsonProperty("message_id")
        public void setMessageId(int messageId) {
            this.messageId = messageId;
        }
    }
}