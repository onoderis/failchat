package failchat.sc2tv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Smile;
import failchat.core.SmileManager;
import failchat.core.SmileSource;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Sc2tvSmile extends Smile {

    Sc2tvSmile() {
        source = SmileSource.SC2TV;
    }

    @Override
    @JsonProperty(value = "code")
    public void setCode(String code) {
        this.code = code;
    }

    @Override
    @JsonProperty(value = "img")
    public void setImageUrl(String url) {
        this.imageUrl = SmileManager.SC2TV_SMILES_DIR_URL + url;
    }

}
