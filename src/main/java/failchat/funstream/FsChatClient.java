package failchat.funstream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import failchat.core.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

public class FsChatClient implements ChatClient {
    private static final Logger logger = Logger.getLogger(FsChatClient.class.getName());
    private static final String FS_SOCKET_URL = "http://funstream.tv:3811";

    private ChatClientStatus status;
    private MessageManager messageManager = MessageManager.getInstance();
    private Queue<failchat.core.Message> messageQueue = messageManager.getMessagesQueue();
    private List<MessageHandler<FsMessage>> messageHandlers;
    private List<MessageFilter<FsMessage>> messageFilters;
    private ObjectMapper objectMapper;
    private String channelName;
    private int channelId;
    private Socket socket;

    public FsChatClient(String channelName) {
        this.channelName = channelName;
        messageHandlers = new ArrayList<>();
        messageFilters = new ArrayList<>();
        objectMapper = new ObjectMapper();
        messageFilters.add(new SourceFilter());
        //noinspection unchecked
        messageHandlers.add(MessageObjectCleaner.getInstance());
        messageHandlers.add(new FsHighlightHandler(channelName));
        messageHandlers.add(new FsSmileHandler());
        status = ChatClientStatus.READY;
    }

    @Override
    public void goOnline() {
        if (status != ChatClientStatus.READY) {
            return;
        }

        new Thread(() -> {
            channelId = FsApiWorker.getChannelIdByName(channelName);
            if (channelId == -1) {
                logger.warning("Can't load funstream channel id");
                return;
            }
            socket = buildSocket();
            socket.connect();
        }).run();
    }

    @Override
    public void goOffline() {
        status = ChatClientStatus.SHUTDOWN;
        if (socket != null) {
            socket.disconnect();
        }
    }

    @Override
    public ChatClientStatus getStatus() {
        return status;
    }

    private Socket buildSocket() {
        try {
            URI chatApiUrl = URI.create(FS_SOCKET_URL);
            IO.Options options = new IO.Options();
            options.transports = new String[1];
            options.forceNew = true;
            options.transports[0] = "websocket";

            Socket socket =  IO.socket(chatApiUrl, options);
            socket.on(Socket.EVENT_CONNECT, objects -> {
                if (status == ChatClientStatus.SHUTDOWN) { //for quick close case
                    socket.disconnect();
                    return;
                }
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("channel", "stream/" + channelId);
                    Object[] args1 = new Object[1];
                    args1[0] = obj;
                    socket.emit("/chat/join", args1, rObjects -> {
                        logger.info("Connected to funstreams");
                        messageManager.sendInfoMessage(new InfoMessage(Source.SC2TV, "connected"));
                        status = ChatClientStatus.WORKING;
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }).on(Socket.EVENT_DISCONNECT, objects -> {
                status = ChatClientStatus.CONNECTING;
                logger.info("Disconnected from funstreams");
                messageManager.sendInfoMessage(new InfoMessage(Source.SC2TV, "disconnected"));
            }).on("/chat/message", objects -> {
                try {
                    FsMessage fsMessage = objectMapper.readValue(objects[0].toString(), FsMessage.class);

                    //filter message
                    for (MessageFilter<FsMessage> messageFiller : messageFilters) {
                        if (messageFiller.filterMessage(fsMessage)) {
                            return;
                        }
                    }
                    //handle message
                    for (MessageHandler<FsMessage> messageHandler : messageHandlers) {
                        messageHandler.handleMessage(fsMessage);
                    }
                    messageQueue.add(fsMessage);
                    synchronized (messageQueue) {
                        messageQueue.notify();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            return socket;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}