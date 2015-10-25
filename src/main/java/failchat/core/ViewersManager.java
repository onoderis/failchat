package failchat.core;

import failchat.cybergame.CgViewersCounter;
import failchat.goodgame.GGViewersCounter;
import failchat.twitch.TwitchViewersCounter;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ViewersManager implements Runnable {
    private static final Logger logger = Logger.getLogger(ViewersManager.class.getName());

    private static final int timeout = 15000;
    private Map<Source, ViewersCounter> enabledSources = new HashMap<>();
    private MessageManager messageManager = MessageManager.getInstance();
    private Status status = Status.READY;
    private final Object lock = new Object();

    @Override
    public void run() {
        while (status != Status.SHUTDOWN) {
            enabledSources.values().forEach(failchat.core.ViewersCounter::updateViewersCount);
            messageManager.sendViewersMessage(getData());
            try {
                synchronized (lock) {
                    if (status == Status.SHUTDOWN) break;
                    lock.wait(timeout);
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Something goes wrong...", e);
            }
        }
        status = Status.READY;
    }

    public synchronized void start(Set<Source> sources) {
        if (status != Status.READY) {
            return;
        }

        enabledSources.clear();
        if (sources.contains(Source.TWITCH)) {
            enabledSources.put(Source.TWITCH, new TwitchViewersCounter(Configurator.config.getString("twitch.channel")));
        }
        if (sources.contains(Source.GOODGAME)) {
            enabledSources.put(Source.GOODGAME, new GGViewersCounter(Configurator.config.getString("goodgame.channel")));
        }
        if (sources.contains(Source.CYBERGAME)) {
            enabledSources.put(Source.CYBERGAME, new CgViewersCounter(Configurator.config.getString("cybergame.channel")));
        }

        status = Status.WORKING;
        new Thread(this, "ViewersCounterThread").start();
        logger.info("ViewersManager started");
    }

    public synchronized void stop() {
        if (status == Status.READY) {
            return;
        }
        status = Status.SHUTDOWN;
        synchronized (lock) {
            lock.notify();
        }
        messageManager.sendViewersMessage(getData());
        logger.info("ViewersManager stopped");
    }

    public JSONObject getData() {
        JSONObject mes = new JSONObject();
        try {
            mes.put("type", "viewers");
            JSONObject content = new JSONObject();
            boolean show = Configurator.config.getBoolean("showViewers");
            content.put("show", show);
            if (show) {
                if (status == Status.WORKING) {
                    for (Map.Entry<Source, ViewersCounter> entry : enabledSources.entrySet()) {
                        int viewers = entry.getValue().getViewersCount();
                        if (viewers >= 0) {
                            content.put(entry.getKey().getLowerCased(), viewers);
                        } else {
                            content.put(entry.getKey().getLowerCased(), "?");
                        }
                    }
                //если запрос приходит до запуска ViewersManager'а
                } else if (status == Status.READY) {
                    if (Configurator.config.getBoolean("goodgame.enabled") && !Configurator.config.getString("goodgame.channel").equals("")) {
                        content.put(Source.GOODGAME.getLowerCased(), "?");
                    }
                    if (Configurator.config.getBoolean("twitch.enabled") && !Configurator.config.getString("twitch.channel").equals("")) {
                        content.put(Source.TWITCH.getLowerCased(), "?");
                    }
                    if (Configurator.config.getBoolean("cybergame.enabled") && !Configurator.config.getString("cybergame.channel").equals("")) {
                        content.put(Source.CYBERGAME.getLowerCased(), "?");
                    }
                }
            }
            mes.put("content", content);
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Something goes wrong...", e);
        }
        return mes;
    }

    private static enum Status {
        READY,
        WORKING,
        SHUTDOWN
    }
}
