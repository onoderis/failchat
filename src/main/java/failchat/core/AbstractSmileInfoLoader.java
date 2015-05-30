package failchat.core;

import java.util.Map;

/**
 * Абстрактный класс для SmileInfoLoader'ов, которые загружают информацию о смайлах, доступных в чате.
 */
public abstract class AbstractSmileInfoLoader {
    protected Map<String, Smile> smiles;

    abstract public void loadSmilesInfo();

    public Map<String, Smile> getSmiles() {
        return smiles;
    }

    //На случай разных форматов изображения смайла. Как правило это .png
    abstract public String getFileExtension();
}
