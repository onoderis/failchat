package failchat.handlers;

import failchat.core.MessageHandler;
import failchat.core.Smile;
import failchat.core.SmileManager;
import failchat.core.Source;
import failchat.funstream.FsMessage;

public class SupportSmileHandler implements MessageHandler<FsMessage> {
    private static final Smile supportSmile = new Smile(){
        {
            code = "icon_donate";
            source = Source.SC2TV;
        }

        @Override
        public String getImageUrl() {
            return "http://funstream.tv/build/images/icon_donate.png";
        }

        @Override
        public String getFileName() {
            return "icon_donate.png" ;
        }

        @Override
        public String getCachePath() {
            return SmileManager.SMILES_DIR_REL.resolve(Source.SC2TV.getLowerCased())
                    .resolve(getFileName()).toString().replace('\\', '/');
        }
    };

    @Override
    public void handleMessage(FsMessage message) {
        if (message.getType().equals("fastdonate") && SmileManager.cacheSmile(supportSmile)) {
            message.setText(message.addSmile(supportSmile));
        }
    }
}
