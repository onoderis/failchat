package failchat.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class MessageManager implements Runnable {

    private static final Logger logger = Logger.getLogger(MessageManager.class.getName());
    private final Queue<Message> messages = new ConcurrentLinkedQueue<>();
    private boolean exitFlag = false;
    private LocalWSServer localWSServer;
    private List<MessageHandler> handlers = new ArrayList<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run() {
        initHandlers();
        createWebSocket();
        checkMessagesLoop();
    }

    public void turnOff() {
        try {
            localWSServer.stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exitFlag = true;
        synchronized (messages) {
            messages.notify();
        }
    }

    public Queue<Message> getMessagesQueue() {
        return messages;
    }

    private void initHandlers() {
        // обработка ссылок
    }

    private void createWebSocket() {
        localWSServer = new LocalWSServer();
        localWSServer.start();
    }

    private void checkMessagesLoop() {
        while (!exitFlag) {
            checkNewMessages();
            try {
                synchronized (messages) {
                    messages.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkNewMessages() {
        if (messages.size() > 0) {
            while (!messages.isEmpty()) {
                Message m = messages.poll();
                for (MessageHandler h : handlers) {
                    h.handleMessage(m);
                }
                try {
                    String jsonMessage = objectMapper.writeValueAsString(new LocalCommonMessage(m));
                    logger.fine("Send to Web Socket: " + jsonMessage);
                    localWSServer.sendToAll(jsonMessage);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
