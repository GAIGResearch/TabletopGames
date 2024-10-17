package games.hanabi;

import java.awt.*;

public enum CardType {
    Red("Red", Color.red),
    Yellow("Yellow", Color.yellow),
    Green("Green", Color.green),
    White("White", Color.white),
    Blue("Blue", Color.blue);

    public final Color color;
    public final String colorStr;

    CardType(String colorStr, Color color) {
        this.color = color;
        this.colorStr = colorStr;
    }
}
