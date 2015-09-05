package failchat.goodgame;

import failchat.core.ViewersCounter;

public class GGViewersCounter implements ViewersCounter {
    private String channel;
    private int viewers = -1;

    public GGViewersCounter(String channel) {
        this.channel = channel;
    }

    @Override
    public int getViewersCount() {
        return viewers;
    }

    @Override
    public void updateViewersCount() {
        viewers = GGApiWorker.getViewersCount(channel);
    }
}
