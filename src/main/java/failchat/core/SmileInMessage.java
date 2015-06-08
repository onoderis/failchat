package failchat.core;


/**
* Класс, сериализуемый в json для передачи к локальному клиенту
* */

public class SmileInMessage {
    protected int position;
    protected Smile smile;

    public SmileInMessage(Smile s, int position) {
        this.position = position;
        this.smile = s;
    }

    public int getPosition() {
        return position;
    }

    public Smile getSmile() {
        return smile;
    }
}
