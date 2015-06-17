package failchat.sc2tv;

import failchat.core.Message;
import failchat.core.MessageHandler;
import failchat.core.Smile;
import failchat.core.SmileManager;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Находит в тексте код самйла, удаляет его из текста и добавляет объект SmileInMessage в Message
*/
public class Sc2tvSmileHandler implements MessageHandler {

    private static final Pattern sc2tvSmilePattern = Pattern.compile(":s:(\\w*?):");

    private Map<String, Smile> smiles = Sc2tvSmileInfoLoader.loadSmilesInfo();

    @Override
    public void handleMessage(Message message) {
        if (smiles == null) {
            return;
        }
        Matcher matcher = sc2tvSmilePattern.matcher(message.getText());
        while (matcher.find()) {
            Smile smile = smiles.get(matcher.group(1));
            if (smile != null) {
                String num = message.addSmile(smile);
                message.setText(matcher.replaceFirst(num));
                matcher = sc2tvSmilePattern.matcher(message.getText());
                SmileManager.cacheSmile(smile);
            }
        }
    }
}

