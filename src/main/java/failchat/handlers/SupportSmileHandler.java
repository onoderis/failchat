package failchat.handlers;

import failchat.core.*;

public class SupportSmileHandler implements MessageHandler {
    private static final Smile supportSmile = new Smile(){
        {
            code = "support_smile";
            source = Source.SC2TV;
        }

        @Override
        public String getImageUrl() {
            return "http://sc2tv.ru/sites/all/modules/sc2tv_streams_donate/images/dollar_small.gif";
        }

        @Override
        public String getFileName() {
            return "dollar_small.gif";
        }

        @Override
        public String getCachePath() {
            return SmileManager.SMILES_DIR_REL.resolve(Source.SC2TV.getLowerCased())
                    .resolve(getFileName()).toString().replace('\\', '/');
        }
    };
    private static final String supportSmileMessage = supportSmile.getImageUrl();

    @Override
    public void handleMessage(Message message) {
        if (message.getText().contains("<") && message.getText().contains(supportSmileMessage)) { //could be faked in funstream chat
            if (SmileManager.cacheSmile(supportSmile)) {
                message.setText(message.addSmile(supportSmile));
            }

        }
    }
}
