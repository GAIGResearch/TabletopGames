package utilities;

import java.awt.*;

public abstract class Utils {

    public static Color stringToColor(String c) {
        switch(c.toLowerCase()) {
            case "blue": return Color.BLUE;
            case "black": return Color.BLACK;
            case "yellow": return Color.YELLOW;
            case "red": return Color.RED;
            case "green": return Color.GREEN;
            default: return null;
        }
    }

    public enum ComponentType {
        BOARD,
        DECK,
        CARD,
        COUNTER,
        DICE,
        TOKEN;
    }

    public static int indexOf (String[] array, String object) {
        for (int i = 0; i < array.length; i++) {
            if (object.equals(array[i])) {
                return i;
            }
        }
        return -1;
    }
}
