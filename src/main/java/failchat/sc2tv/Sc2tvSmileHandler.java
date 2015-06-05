package failchat.sc2tv;

import failchat.core.*;

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
                //заменяем текст на объекты в сообщении
                int position = matcher.start();
                message.setText(matcher.replaceFirst(""));
                matcher = sc2tvSmilePattern.matcher(message.getText());

                //кешируем если надо
                SmileManager.cacheSmile(smile);
                SmileInMessage smileInMessage = new SmileInMessage(smile, position);

                message.getSmileList().add(smileInMessage);
            }
        }
    }
}

