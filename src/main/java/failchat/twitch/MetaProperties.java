package failchat.twitch;

/**
 * Содержит информацию, полученную из мета сообщений.
 * Свойства: USERCOLOR, EMOTESET, SPECIALUSER
 */
public class MetaProperties {
    private int[] emoteSets = null;

    public int[] getEmoteSets() {
        return emoteSets;
    }

    public void setEmoteSets(int[] emoteSets) {
        this.emoteSets = emoteSets;
    }
}
