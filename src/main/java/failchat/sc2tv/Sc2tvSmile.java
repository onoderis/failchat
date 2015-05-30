package failchat.sc2tv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Smile;
import failchat.core.Source;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Sc2tvSmile extends Smile {
    public static final String FILE_EXTENSION = ".png";

    public Sc2tvSmile() {
        source = Source.SC2TV;
    }

    // удаляем из кода символ :
    @Override
    @JsonProperty(value = "code")
    public void setCode(String code) {
        this.code = code.replaceAll("\\:", "");
    }

    // генерируем ссылку на картинку
    @Override
    @JsonProperty(value = "img")
    public void setImageUrl(String img) {
        this.imageUrl = Sc2tvSmileInfoLoader.SC2TV_SMILES_DIR_URL + img;
        this.fileName = img.split("\\?")[0]; // smile.png?1 -> smile.png
    }
}
