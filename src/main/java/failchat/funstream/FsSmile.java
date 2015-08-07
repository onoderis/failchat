package failchat.funstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Smile;
import failchat.core.SmileManager;
import failchat.core.Source;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FsSmile extends Smile {
    private static final String FS_SMILES_DIR = "http://funstream.tv/build/images/smiles/";
    private static final String FILE_EXTENSION = ".png";

    private String urlFileName;

    FsSmile() {
        source = Source.FUNSTREAM;
    }

    @Override
    public String getImageUrl() {
        return FS_SMILES_DIR + urlFileName;
    }

    @Override
    public String getCachePath() {
        return SmileManager.SMILES_DIR_REL.resolve(Source.FUNSTREAM.getLowerCased())
                .resolve(code + FILE_EXTENSION).toString().replace('\\', '/');
    }

    @JsonProperty(value = "image")
    public void setFileName(String image) {
        this.urlFileName = image.split("\\?")[0]; // smile.png?1 -> smile.png
    }

    @Override
    public String getFileName() {
        return code + FILE_EXTENSION;
    }
}
