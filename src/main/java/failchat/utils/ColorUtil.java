package failchat.utils;

import javafx.scene.paint.Color;

public class ColorUtil {
    public static Color getOpaque(Color color) {
        if (color.isOpaque()) {
            return color;
        }
        else {
            return Color.color(color.getRed(), color.getGreen(), color.getBlue());
        }
    }
}
