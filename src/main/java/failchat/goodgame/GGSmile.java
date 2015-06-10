package failchat.goodgame;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Smile;
import failchat.core.SmileManager;
import failchat.core.Source;

@JsonIgnoreProperties (ignoreUnknown = true)
public class GGSmile extends Smile {
    public static final String FILE_EXTENSION = ".png";
    public static final String IMG_DIR_URL = "http://goodgame.ru/images/smiles/";
    public static final String IMG_DIR_URL_END = "-big";


    @Override
    public String getImageUrl() {
        return IMG_DIR_URL + code + IMG_DIR_URL_END + FILE_EXTENSION;
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
        return SmileManager.SMILES_DIR_REL.resolve(Source.GOODGAME.toString().toLowerCase())
                .resolve(code + FILE_EXTENSION).toString();
    }

    @Override
    public String getFileName() {
        return code + FILE_EXTENSION;
    }
}
