package failchat.test;

import failchat.core.*;

import java.util.Queue;

public class TestChatClient implements ChatClient, Runnable {

    private final Queue<Message> messageQueue;
    boolean exitFlag = false;

    public TestChatClient(Queue<Message> queue) {
        messageQueue = queue;
    }

    @Override
    public void goOffline() {
        exitFlag = true;
    }

    @Override
    public void goOnline() {
        Thread t = new Thread(this, "TestChatClient");
        t.start();
    }

    @Override
    public ChatClientStatus getStatus() {
        return null;
    }

    @Override
    public void run() {
        int i = 0;
        while (!exitFlag) {
            Message m = new Message();
            m.setAuthor("Test author");
            m.setText("test text " + i);
            if (i % 3 == 0) {
                m.setHighlighted(true);
            }
//            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//            try {
//                m.setText(br.readLine());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            messageQueue.add(m);
            synchronized (messageQueue) {
                messageQueue.notify();
            }

            Source[] sources = Source.values();
            if (i % 5 == 0) {
                MessageManager.getInstance().sendInfoMessage(new InfoMessage(sources[(i/5)%sources.length], "test info message " + i));
            }
            i++;
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
