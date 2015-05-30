package failchat.core;

import java.util.Queue;

public class TestChatClient implements ChatClient, Runnable {

    private final Queue<Message> messageQueue;
    boolean exitFlag = false;

    TestChatClient(Queue<Message> queue) {
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
            m.setSource(Source.TEST);
            m.setAuthor("Test author");
            m.setText("test text " + i);
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
            i++;
            synchronized (this) {
                try {
                    wait(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
