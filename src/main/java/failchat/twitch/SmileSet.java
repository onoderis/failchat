package failchat.twitch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SmileSet {
    private int id;
    private Pattern pattern; //pattern for all smiles in set
    private Map<String, TwitchSmile> smiles;

    public SmileSet(int id, Map<String, TwitchSmile> smiles) {
        this.id = id;
        this.smiles = smiles;
        StringBuilder builder = new StringBuilder();
        for (HashMap.Entry<String, TwitchSmile> smile : smiles.entrySet()) {
            builder.append("(\\b");
            builder.append(smile.getKey());
            builder.append("\\b)|");
        }
        builder.deleteCharAt(builder.length() - 1);
        this.pattern = Pattern.compile(builder.toString());
    }

    public int getId() {
        return id;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Map<String, TwitchSmile> getSmiles() {
        return smiles;
    }

    public void setId(int id) {
        this.id = id;
    }
}
