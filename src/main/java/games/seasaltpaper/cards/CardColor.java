package games.seasaltpaper.cards;

import java.awt.*;

public enum CardColor
{
    YELLOW(new Color(248, 220, 111)),
    RED(new Color(239, 141, 141)),
    BLUE(new Color(90, 188, 243)),
    ORANGE(new Color(238, 130, 73)),
    GREEN(new Color(91, 227, 144)),
    PURPLE(new Color(210, 128, 246)),
    GREY(new Color(175, 168, 168));   // Grey for Multiplier and Mermaid cards

    private final Color color;

    CardColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}

