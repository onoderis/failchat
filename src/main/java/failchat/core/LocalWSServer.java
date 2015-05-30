package failchat.core;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collection;

public class LocalWSServer extends WebSocketServer {

    private static final int WS_PORT = 8887;

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

    public void sendToAll(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }
}

//@ServerEndpoint(value = "/")
//public class LocalChatServer {
//
//    private static final int WS_PORT = 8887;
//
//    private Server server;
//    private ArrayList<Session> sessions = new ArrayList<>();
//
//    LocalChatServer() {
//        server = new Server("localhost", WS_PORT, null, null, LocalChatServer.class);
//    }
//
//    public void start() {
//        try {
//            server.start();
//        } catch (DeploymentException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void stop() {
//        server.stop();
//    }
//
//    @OnOpen
//    public void onOpen(Session session) {
//        sessions.add(session);
//    }
//
//    @OnClose
//    public void onClose (Session session) {
//        sessions.remove(session);
//    }
//
//    @OnMessage
//    public void onMessage (Session session) {
//        System.out.println("message");
//    }
//
//    public void sendToAll(String text) {
//        for (Session s : sessions) {
//            if (s.isOpen()) {
//                try {
//                    s.getBasicRemote().sendText(text);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//}