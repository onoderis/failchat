package failchat.core;


/**
* Класс, десериализуемый в json для передачи к локальному клиенту
* */

public class SmileInMessage extends Smile {
    protected int position;

    public SmileInMessage(Smile s, int position) {
        if (s.cache != null) {
            this.imageUrl = s.getCache();
        } else {
            this.imageUrl = s.getImageUrl();
        }
        this.code = s.getCode();
        this.source = s.getSource();
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
