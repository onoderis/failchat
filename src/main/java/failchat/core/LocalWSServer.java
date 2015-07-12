package failchat.core;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.logging.Logger;

public class LocalWSServer extends WebSocketServer {
    public static final int WS_PORT = 10880;

    private static final Logger logger = Logger.getLogger(LocalWSServer.class.getName());

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

    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    synchronized public void sendToAll(String text) {
        logger.fine("Send to Web Socket: " + text);
        Collection<WebSocket> con = connections();
            for (WebSocket c : con) {
                c.send(text);
            }
    }
}
