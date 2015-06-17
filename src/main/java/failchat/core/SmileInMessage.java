package failchat.core;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
* Класс, сериализуемый в json для передачи к локальному клиенту
* */

public class SmileInMessage {
    protected int objectNumber; //starts from 1
    protected Smile smile;

    public SmileInMessage(Smile s, int objectNumber) {
        this.objectNumber = objectNumber;
        this.smile = s;
    }

    public int getObjectNumber() {
        return objectNumber;
    }

    public Source getSource() {
        return smile.getSource();
    }

    @JsonProperty(value = "code")
    public String getCode() {
        return smile.getCode();
    }

    @JsonProperty (value = "imgUrl")
    public String getCachePath() {
        return smile.getCachePath();
    }

}
