package failchat.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import failchat.funstream.FsMessage;
import failchat.goodgame.GGMessage;
import failchat.handlers.IgnoreFilter;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Moderation {
    private static volatile Moderation instance;
    private static final Logger logger = Logger.getLogger(Moderation.class.getName());

    private MessageManager messageManager = MessageManager.getInstance();
    private MessageHistory messageHistory = MessageHistory.getInstance();
    private IgnoreFilter ignoreFilter = messageManager.getIgnoreFilter();
    private ObjectMapper objectMapper = new ObjectMapper();

    private Moderation() {}

    public static Moderation getInstance() {
        Moderation localInstance = instance;
        if (localInstance == null) {
            synchronized (Moderation.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new Moderation();
                }
            }
        }
        return localInstance;
    }

    public void processDeleteMessage(JSONObject message) {
        try {
            deleteMessage(message.getInt("messageId"));
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Something goes wrong...", e);
        }
    }

    public void processIgnoreMessage(JSONObject message) {
        try {
            ignoreFilter.ignore(message.getString("user"));
            deleteMessage(message.getInt("messageId"));
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Something goes wrong...", e);
        }
    }

    public void deleteMessage(Message message) {
        try {
            messageManager.sendRaw(objectMapper.writeValueAsString(new ModMessage(message.getId())));
            logger.fine("Message deleted: " + message.source + "#" + message.getAuthor() + ": " + message.getText());
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Something goes wrong...", e);
        }
    }

    public void deleteMessage(int messageId) {
        try {
            messageManager.sendRaw(objectMapper.writeValueAsString(new ModMessage(messageId)));
            logger.fine("Message deleted by id: " + messageId);
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Something goes wrong...", e);
        }
    }

    public void deleteFsMessage(int fsMessageId) {
        FsMessage message = messageHistory.findFsMessage(fsMessageId);
        if (message != null) {
            deleteMessage(message);
        }
    }

    public void deleteGgMessage(int ggMessageId) {
        GGMessage message = messageHistory.findGGMessage(ggMessageId);
        if (message != null) {
            deleteMessage(message);
        }
    }

    public void deleteTwitchMessages(String bannedUser) {
        messageHistory.findTwitchMessages(bannedUser).forEach(this::deleteMessage);
    }


    private static class ModMessage {
        public String type = "mod";
        public ModMessageContent content;

        ModMessage (int messageId) {
            content = new ModMessageContent(messageId);
        }
    }

    private static class ModMessageContent {
        public int messageId;

        ModMessageContent(int messageId) {
            this.messageId = messageId;
        }
    }

}
