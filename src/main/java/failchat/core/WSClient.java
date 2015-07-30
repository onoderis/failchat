package failchat.core;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WSClient {
    private WebSocketClient wsClient;
    private String uri;
    private int reconnectTimeout = 5000;
    ChatClientStatus status = ChatClientStatus.READY;

    public WSClient(String uri) {
        this.uri = uri;
    }

    //non-blocking method
    public void connect() {
        new Thread(() -> {
            while (!(status == ChatClientStatus.SHUTDOWN || status == ChatClientStatus.ERROR)) {
                wsClient = new Wsc(URI.create(uri));
                try {
                    if (wsClient.connectBlocking()) {
                        synchronized (wsClient) {
                            wsClient.wait();
                        }
                    } else {
                        Thread.sleep(reconnectTimeout);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }, "WSReconnectThread").start();
    }

    public void close() {
        status = ChatClientStatus.SHUTDOWN;
        if (wsClient != null) {
            wsClient.close();
        }
        synchronized (wsClient) {
            wsClient.notify();
        }
    }

    public void send(String s) {
        if (status == ChatClientStatus.WORKING) {
            wsClient.send(s);
        }
    }


    public void onOpen(ServerHandshake serverHandshake) {}

    public void onMessage(String s) {}

    public void onClose(int i, String s, boolean b) {}

    public void onReconnect() {}

    private class Wsc extends WebSocketClient {
        public Wsc(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            failchat.core.WSClient.this.status = ChatClientStatus.WORKING;
            failchat.core.WSClient.this.onOpen(serverHandshake);
        }

        @Override
        public void onMessage(String s) {
            failchat.core.WSClient.this.onMessage(s);
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            if (failchat.core.WSClient.this.status == ChatClientStatus.CONNECTING || failchat.core.WSClient.this.status == ChatClientStatus.SHUTDOWN
                    || failchat.core.WSClient.this.status == ChatClientStatus.ERROR) {
                return;
            }
            if (failchat.core.WSClient.this.status == ChatClientStatus.WORKING){
                failchat.core.WSClient.this.status = ChatClientStatus.CONNECTING;
                failchat.core.WSClient.this.onReconnect();
            }
            failchat.core.WSClient.this.onClose(i, s, b);
            synchronized (this) {
                this.notify();
            }
        }

        @Override
        public void onError(Exception e) {
            if (failchat.core.WSClient.this.status == ChatClientStatus.WORKING){
                failchat.core.WSClient.this.status = ChatClientStatus.CONNECTING;
                failchat.core.WSClient.this.onReconnect();
            }
        }
    }
}
