package failchat.goodgame;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Smile;
import failchat.core.SmileManager;
import failchat.core.Source;

@JsonIgnoreProperties (ignoreUnknown = true)
public class GGSmile extends Smile {
    public static final String IMG_DIR_URL = "http://goodgame.ru/images/smiles/";
    public static final String IMG_DIR_URL_END = "-big";
    public static final String FILE_EXTENSION = ".png";
    public static final String A_IMG_DIR_URL = "http://goodgame.ru/images/anismiles/";
    public static final String A_IMG_DIR_URL_END = "-gif";
    public static final String A_FILE_EXTENSION = ".gif";

    protected boolean premium;
    protected boolean animated;
    protected GGSmile animatedInstance;

    @Override
    public String getImageUrl() {
        if (animated) {
            return A_IMG_DIR_URL + code + A_IMG_DIR_URL_END + A_FILE_EXTENSION;
        }
        else {
            return IMG_DIR_URL + code + IMG_DIR_URL_END + FILE_EXTENSION;
        }
    }

    @Override
    public Source getSource() {
        return Source.GOODGAME;
    }

    @Override
    @JsonProperty (value = "name")
    public void setCode(String code) {
        super.setCode(code);
    }

    @Override
    public String getCachePath() {
        if (animated) {
            return SmileManager.SMILES_DIR_REL.resolve(Source.GOODGAME.getLowerCased())
                    .resolve(code + A_FILE_EXTENSION).toString().replace('\\', '/');
        }
        else {
            return SmileManager.SMILES_DIR_REL.resolve(Source.GOODGAME.getLowerCased())
                    .resolve(code + FILE_EXTENSION).toString().replace('\\', '/');
        }
    }

    @Override
    public String getFileName() {
        if (animated) {
            return code + A_FILE_EXTENSION;
        }
        else {
            return code + FILE_EXTENSION;
        }
    }

    @JsonIgnore
    public boolean isPremium() {
        return premium;
    }

    @JsonProperty (value = "premium")
    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    @JsonIgnore
    public boolean isAnimated() {
        return animated;
    }

    @JsonProperty (value = "animated")
    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    public GGSmile getAnimatedInstance() {
        return animatedInstance;
    }

    public void setAnimatedInstance(GGSmile animatedInstance) {
        this.animatedInstance = animatedInstance;
    }
}
