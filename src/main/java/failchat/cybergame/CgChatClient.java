package failchat.cybergame;

import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.core.*;
import failchat.handlers.CommonHighlightHandler;
import failchat.handlers.LinkHandler;
import failchat.handlers.MessageObjectCleaner;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CgChatClient implements ChatClient {

    private static final Logger logger = Logger.getLogger(CgChatClient.class.getName());
    private static final String CG_WS_URL = "ws://cybergame.tv:9090/websocket";

    private WSClient wsClient;
    private ChatClientStatus status;
    private MessageManager messageManager = MessageManager.getInstance();
    private List<MessageHandler<CgMessage>> messageHandlers;
    private String channelName;
    private ObjectMapper objectMapper;

    public CgChatClient(String channelName) {
        this.channelName = channelName;
        objectMapper = new ObjectMapper();
        messageHandlers = new ArrayList<>();
        messageHandlers.add(MessageObjectCleaner.getInstance());
        messageHandlers.add(new LinkHandler());
        messageHandlers.add(new CommonHighlightHandler(channelName));
        status = ChatClientStatus.READY;
    }

    @Override
    public void goOnline() {
        if (status != ChatClientStatus.READY) {
            return;
        }
        wsClient = new CgWSClient(CG_WS_URL);
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


    private class CgWSClient extends WSClient {
        CgWSClient(String uri) {
            super(uri);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            try {
                JSONObject loginMessage = new JSONObject();
                loginMessage.put("command", "command");
                JSONObject loginData = new JSONObject();
                loginData.put("login", "");
                loginData.put("password", "");
                loginData.put("channel", "#" + channelName);
                loginMessage.put("message", loginData.toString());
                send(loginMessage.toString());
            } catch (JSONException e) {
                logger.log(Level.WARNING, "Something goes wrong...", e);
            }
        }

        @Override
        public void onMessage(String s) {
            try {
                CgWSMessage commonMessage = objectMapper.readValue(s, CgWSMessage.class);
                if (commonMessage.getCommand().equals("chatMessage")) {
                    CgMessage message = objectMapper.readValue(commonMessage.getMessage().toString(), CgMessage.class);

                    //handling messages
                    for (MessageHandler<CgMessage> messageHandler : messageHandlers) {
                        messageHandler.handleMessage(message);
                    }
                    messageManager.sendMessage(message);
                }
                else if (commonMessage.getCommand().equals("changeWindow")) {
                    logger.info("Connected to cybergame");
                    messageManager.sendInfoMessage(new InfoMessage(Source.CYBERGAME, "connected"));
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Something goes wrong...", e);
            }
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            logger.info("Cybergame disconnected");
        }

        @Override
        public void onError(Exception e) {
            logger.log(Level.WARNING, "Disconnect? ...", e);
        }

        @Override
        public void onReconnect() {
            logger.info("Cybergame disconnected, trying to reconnect ...");
            messageManager.sendInfoMessage(new InfoMessage(Source.CYBERGAME, "disconnected"));
        }
    }

    private static class CgWSMessage {
        private String command;
        private Object message;

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public Object getMessage() {
            return message;
        }

        public void setMessage(Object message) {
            this.message = message;
        }
    }
}
