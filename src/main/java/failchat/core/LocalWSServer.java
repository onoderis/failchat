package failchat.core;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.nio.channels.NotYetConnectedException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalWSServer extends WebSocketServer {
    public static final int WS_PORT = 10880;

    private static final Logger logger = Logger.getLogger(LocalWSServer.class.getName());
    private WebSocket nativeClient; //javafx webview connection
    private Configurator configurator = Configurator.getInstance();
    private Moderation moderation = Moderation.getInstance();

    public LocalWSServer() {
        super(new InetSocketAddress(WS_PORT));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {

    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        logger.fine("Received from Web Socket: " + s);
        try {
            JSONObject obj = new JSONObject(s);
            switch (obj.getString("type")) {
                case "delete-message": {
                    moderation.processDeleteMessage(obj.getJSONObject("content"));
                    break;
                }
                case "ignore": {
                    moderation.processIgnoreMessage(obj.getJSONObject("content"));
                    break;
                }
                case "viewers": {
                    nativeClient = webSocket;
                    nativeClient.send(configurator.getViewersManager().getData().toString());
                    break;
                }
            }
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Something goes wrong...", e);
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    synchronized public void sendToAll(String text) {
        logger.fine("Send to Web Socket: " + text);
        Collection<WebSocket> con = connections();
        try {
            for (WebSocket c : con) {
                c.send(text);
            }
        } catch (NotYetConnectedException e) {
            logger.log(Level.WARNING, "Send message to disconnected client...", e);
        }
    }

    public void sendToNativeClient(String text) {
        if (nativeClient != null && nativeClient.isOpen()) {
            logger.fine("Send to native client: " + text);
            nativeClient.send(text);
        }
    }
}
