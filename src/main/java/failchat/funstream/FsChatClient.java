package failchat.funstream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import failchat.core.*;
import failchat.handlers.CapsHandler;
import failchat.handlers.HtmlHandler;
import failchat.handlers.MessageObjectCleaner;
import failchat.handlers.SupportSmileHandler;
import failchat.utils.Array;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FsChatClient implements ChatClient, ViewersCounter {

    private static final Logger logger = Logger.getLogger(FsChatClient.class.getName());
    private static final String FS_SOCKET_URL = "http://chat.funstream.tv";

    private ChatClientStatus status;
    private MessageManager messageManager = MessageManager.getInstance();
    private Moderation moderation = Moderation.getInstance();
    private List<MessageHandler<FsMessage>> messageHandlers;
    private List<MessageFilter<FsMessage>> messageFilters;
    private ObjectMapper objectMapper;
    private String channelName;
    private int channelId;
    private Socket socket;
    private CompletableFuture<Integer> viewersCountFuture = new CompletableFuture<>(); //create future to avoid NPE
    volatile private int viewersCount = -1;

    public FsChatClient(String channelName) {
        this.channelName = channelName;
        messageHandlers = new ArrayList<>();
        messageFilters = new ArrayList<>();
        objectMapper = new ObjectMapper();
        messageFilters.add(new SourceFilter());
        messageFilters.add(new DoubleMessageFilter());
        messageFilters.add(new AnnounceMessageFilter());
        //noinspection unchecked
        messageHandlers.add(MessageObjectCleaner.getInstance());
        messageHandlers.add(new SupportSmileHandler());
        messageHandlers.add(new CapsHandler());
        messageHandlers.add(new HtmlHandler());
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
            options.transports = new String[] {"websocket"};
            options.forceNew = true;

            Socket socket =  IO.socket(chatApiUrl, options);
            socket.on(Socket.EVENT_CONNECT, objects -> {
                if (status == ChatClientStatus.SHUTDOWN) { //for quick close case
                    socket.disconnect();
                    return;
                }
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("channel", "stream/" + channelId);
                    socket.emit("/chat/join", Array.of(obj), rObjects -> {
                        logger.info("Connected to sc2tv");
                        messageManager.sendInfoMessage(new InfoMessage(Source.SC2TV, "connected"));
                        status = ChatClientStatus.WORKING;
                    });
                } catch (JSONException e) {
                    logger.log(Level.WARNING, "Something goes wrong...", e);
                }
            }).on(Socket.EVENT_DISCONNECT, objects -> {
                status = ChatClientStatus.CONNECTING;
                logger.info("Disconnected from sc2tv");
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
                    messageManager.sendMessage(fsMessage);

                } catch (IOException e) {
                    logger.log(Level.WARNING, "Something goes wrong...", e);
                }
            })
                .on("/chat/message/remove", objects -> {
                try {
                    JSONObject removeMessage = (JSONObject) objects[0];
                    moderation.deleteFsMessage(removeMessage.getInt("id"));
                } catch (JSONException e) {
                    logger.log(Level.WARNING, "Something goes wrong...", e);
                }
            });

            return socket;
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Something goes wrong...", e);
            return null;
        }
    }

    @Override
    public int getViewersCount() {
        try {
            viewersCount = viewersCountFuture.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.log(Level.WARNING, "Failed to get viewers count in 1 second", e);
        }
        return viewersCount;
    }

    @Override
    public void updateViewersCount() {
        final CompletableFuture<Integer> viewersCountFuture = this.viewersCountFuture = new CompletableFuture<>();

        JSONObject obj = new JSONObject();
        try {
            obj.put("channel", "stream/" + channelId);
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Illegal channel name", e);
            return;
        }

        socket.emit("/chat/channel/list", Array.of(obj), rObjects -> {
            JSONObject response = (JSONObject) rObjects[0];
            try {
                int newCount;
                if (response.getString("status").equals("ok")) {
                    newCount = response.getJSONObject("result").getInt("amount");
                } else {
                    newCount = -1;
                    logger.warning("Bad response status, json: " + response.toString());
                }
                viewersCountFuture.complete(newCount);
            } catch (JSONException e) {
                logger.log(Level.WARNING, "Unexpected response json format: " + response.toString(), e);
                viewersCountFuture.complete(-1);
            }
        });
    }
}