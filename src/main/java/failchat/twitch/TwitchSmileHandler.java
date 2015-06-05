package failchat.twitch;

import failchat.core.*;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitchSmileHandler implements MessageHandler {

    private Map<String, Smile> smiles;
    private Pattern smilesPattern;


    public TwitchSmileHandler(String channelName) {
        smiles = TwitchSmileInfoLoader.loadSmilesInfo(channelName);
        StringBuilder sb = new StringBuilder();
        for (String code : smiles.keySet()) {
            //TODO: нужно игнорировать, если между кодами смайлов не стоит пробел
            sb.append('(');
            sb.append(code);
            sb.append(")|");
        }
        sb.deleteCharAt(sb.length() - 1);
        smilesPattern = Pattern.compile(sb.toString());
    }

    @Override
    public void handleMessage(Message message) {
//        Matcher m = smilesPattern.matcher(message.getText());
//        while (m.find()) {
//            int position = m.start();
//            String code = m.group();
//            Smile smile = smiles.get(code); // :/
//            SmileManager.cacheSmile(smile);
//            message.setText(message.getText().replaceFirst(code, ""));
//            message.getSmileList().add(new SmileInMessage(smile, position));
//        }

        for (Smile s : smiles.values()) {
            TwitchSmile ts = (TwitchSmile) s;
            Matcher m = ts.getPattern().matcher(message.getText());
            while (m.find()) {
                int position = m.start();
                message.setText(m.replaceFirst(""));
                m = ts.getPattern().matcher(message.getText());
                SmileManager.cacheSmile(ts);
                message.getSmileList().add(new SmileInMessage(ts, position));
            }
        }
    }

    public void handleMetaMessage(String text) {

    }

}
