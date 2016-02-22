package failchat.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.handlers.IgnoreFilter;
import failchat.handlers.LinkHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageManager implements Runnable {

    private static volatile MessageManager instance;
    private static final Logger logger = Logger.getLogger(MessageManager.class.getName());

    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>(); //message queue from chat clients
    private Thread messageManagerThread;
    private LocalWSServer localWSServer;
    private List<MessageHandler> handlers = new ArrayList<>();
    private List<MessageFilter> filters = new ArrayList<>();
    private ObjectMapper objectMapper = new ObjectMapper();
    private LinkHandler linkHandler;
    private IgnoreFilter ignoreFilter;
    private MessageHistory messageHistory = MessageHistory.getInstance();

    private MessageManager() {}

    public static MessageManager getInstance() {
        MessageManager localInstance = instance;
        if (localInstance == null) {
            synchronized (MessageManager.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new MessageManager();
                }
            }
        }
        return localInstance;
    }

    @Override
    public void run() {
        messageManagerThread = Thread.currentThread();
        initializeHandlers();
        createWebSocket();
        takeMessageLoop();
    }

    public void turnOff() {
        try {
            localWSServer.stop();
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Something goes wrong...", e);
        }
        messageManagerThread.interrupt();
    }

    public void sendMessage(Message message) {
        messageQueue.offer(message);
    }

    public void sendInfoMessage(InfoMessage infoMessage) {
        Configurator.InfoMessagesMode mode = Configurator.InfoMessagesMode.getValueByString(Configurator.config.getString("infoMessagesMode"));
        try {
            String jsonMessage = objectMapper.writeValueAsString(new LocalCommonMessage("info", infoMessage));
            if (mode == Configurator.InfoMessagesMode.EVERYWHERE) {
                localWSServer.sendToAll(jsonMessage);
            } else if (mode == Configurator.InfoMessagesMode.ON_NATIVE_CLIENT) {
                localWSServer.sendToNativeClient(jsonMessage);
            }
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Something goes wrong...", e);
        }
    }

    public void sendViewersMessage(JSONObject jsonMessage) {
        localWSServer.sendToNativeClient(jsonMessage.toString());
    }

    public void sendRaw(String rawMessage) {
        localWSServer.sendToAll(rawMessage);
    }

    public IgnoreFilter getIgnoreFilter() {
        return ignoreFilter;
    }

    public LinkHandler getLinkHandler() {
        return linkHandler;
    }

    private void initializeHandlers() {
        linkHandler = new LinkHandler();
        handlers.add(linkHandler);
        ignoreFilter = new IgnoreFilter();
        filters.add(ignoreFilter);
    }

    private void createWebSocket() {
        localWSServer = new LocalWSServer();
        localWSServer.start();
    }

    private void takeMessageLoop() {
        try {
            while (true) {
                try {
                    handleMessage(messageQueue.take());
                } catch (JsonProcessingException e) {
                    logger.log(Level.SEVERE, "Failed to serialize message", e);
                }
            }
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Message manager thread interrupted, shutting down...");
        }
    }

    private void handleMessage(Message message) throws JsonProcessingException {
        for (MessageFilter filter : filters) {
            if (filter.filterMessage(message)) {
                logger.fine("Message filtered: " + message.toString());
                return;
            }
        }
        for (MessageHandler handler : handlers) {
            handler.handleMessage(message);
        }
        String jsonMessage = objectMapper.writeValueAsString(new LocalCommonMessage("message", message));
        localWSServer.sendToAll(jsonMessage);

        messageHistory.addMessage(message);
    }
}
