package failchat.sc2tv;

import failchat.core.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Находит в тексте код самйла, удаляет его из текста и добавляет объект SmileInMessage в Message
*/
public class Sc2tvSmileHandler implements MessageHandler {

    private static final Pattern sc2tvSmilePattern = Pattern.compile(":s:(\\w*?):");

    @Override
    public void handleMessage(Message message) {
        if (message.getSource() != Source.SC2TV) {
            return;
        }

        Matcher matcher = sc2tvSmilePattern.matcher(message.getText());
        while (matcher.find()) {
            Smile smile = SmileManager.getInstance().getSmiles(Source.SC2TV).get(matcher.group(1));
            if (smile != null) {
                //заменяем текст на объекты в сообщении
                int postition = matcher.start();
                message.setText(matcher.replaceFirst(""));
                matcher = sc2tvSmilePattern.matcher(message.getText());

                //кешируем если надо
                long t1 = System.currentTimeMillis();
                SmileManager.getInstance().cacheSmile(smile);
                SmileInMessage smileInMessage = new SmileInMessage(smile, postition);

                message.getSml().add(smileInMessage);
            }
        }
    }
}

