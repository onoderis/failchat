package failchat.funstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Smile;
import failchat.core.SmileManager;
import failchat.core.Source;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FsSmile extends Smile {

    private static final String FILE_EXTENSION = ".png";

    private String imageUrl;

    FsSmile() {
        source = Source.SC2TV;
    }

    @Override
    public void setCode(String code) {
        this.code = code.toLowerCase(); //ignore case
    }

    @Override
    public String getImageUrl() {
        return imageUrl;
    }

    @JsonProperty("url")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl.replace("https://", "http://");
    }

    @Override
    public String getCachePath() {
        return SmileManager.SMILES_DIR_REL.resolve(Source.SC2TV.getLowerCased())
                .resolve(code + FILE_EXTENSION).toString().replace('\\', '/');
    }

    @Override
    public String getFileName() {
        return code + FILE_EXTENSION;
    }
}
