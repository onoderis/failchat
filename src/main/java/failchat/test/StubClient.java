package failchat.test;

import failchat.core.ChatClient;
import failchat.core.ChatClientStatus;

public class StubClient implements ChatClient {
    @Override
    public void goOffline() {

    }

    @Override
    public void goOnline() {

    }

    @Override
    public ChatClientStatus getStatus() {
        return ChatClientStatus.ERROR;
    }
}
