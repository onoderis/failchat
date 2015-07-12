package failchat.twitch;

import failchat.core.MessageHandler;
import failchat.core.SmileManager;

import java.util.logging.Logger;

public class TwitchSmileHandler implements MessageHandler<TwitchMessage> {
    private static final Logger logger = Logger.getLogger(TwitchSmileHandler.class.getName());

    @Override
    public void handleMessage(TwitchMessage message) {
        if (message.getUsedSmiles() == null || message.getUsedSmiles().equals("")) {
            return;
        }

        int smilesCount = 0;

        //parse string
        String rawStr = message.getUsedSmiles(); // 25:6-10/354:0-4,12-16
        String[] smilesStr = rawStr.split("/"); // 354:0-4,12-16
        Smile[] smiles = new Smile[smilesStr.length];
        for (int i = 0; i < smilesStr.length; i++) {
            Smile smile = new Smile();
            smiles[i] = smile;
            String[] t = smilesStr[i].split(":"); //354  |  0-4,12-16
            smile.id = Integer.parseInt(t[0]);
            String[] points = t[1].split(","); // 0-4  | 12-16
            smile.points = new int[points.length][2];
            for (int j = 0; j < points.length; j++) {
                String[] t2 = points[j].split("-");// 0  |  4
                smile.points[j][0] = Integer.parseInt(t2[0]);
                smile.points[j][1] = Integer.parseInt(t2[1]);
                smilesCount++;
            }
            smile.tSmile = TwitchSmileInfoLoader.getSmile(smile.id);
            if (smile.tSmile == null) { // на случай если смайлы ещё не загрузились
                return;
            }
        }

        //out
//        for (Smile smm : smiles) {
//            System.out.println("id: " + smm.id);
//            for (int[] i : smm.points) {
//                for (int j : i) {
//                    logger.info("point " + j);
//                }
//            }
//        }

        StringBuilder sb = new StringBuilder(message.getText());

        for (int i = 0; i < smilesCount; i++) {
            // find max index
            int max = 0;
            int maxInd = 0;
            Smile maxSmile = smiles[0];
            for (Smile sm : smiles) {
                int curMaxInd = sm.findMaxIndex();
                int curMax = sm.points[curMaxInd][0];
                if (curMax >= max) {
                    max = curMax;
                    maxInd = curMaxInd;
                    maxSmile = sm;
                }
            }
            //replace smile text with smile object
            sb.replace(maxSmile.points[maxInd][0], maxSmile.points[maxInd][1] + 1, message.addSmile(maxSmile.tSmile));
            maxSmile.points[maxInd][0] = -1;
            maxSmile.points[maxInd][1] = -1;
        }

        message.setText(sb.toString());
        for (Smile s : smiles) {
            SmileManager.cacheSmile(s.tSmile);
        }
    }

    private static class Smile {
        int id;
        TwitchSmile tSmile;
        int[][] points;

        int findMaxIndex() {
            int max = -1;
            int maxIndex = -1;
            for (int i = 0; i < points.length; i++) {
                if (points[i][0] >= max) {
                    max = points[i][0];
                    maxIndex = i;
                }
            }
            return maxIndex;
        }
    }

}
