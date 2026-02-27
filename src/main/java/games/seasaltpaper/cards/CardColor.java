package games.seasaltpaper.cards;

import java.awt.*;

public enum CardColor
{
    YELLOW(new Color(248, 220, 111)),
    LIGHT_BLUE(new Color(141, 236, 239)),
    BLUE(new Color(63, 153, 243)),
    LIGHT_ORANGE(new Color(248, 175, 136)),
    ORANGE(new Color(253, 116, 46)),
    GREEN(new Color(91, 227, 144)),
    PURPLE(new Color(186, 115, 250)),
    PINK(new Color(255, 105, 239)),
    GREY(new Color(185, 185, 185)),
    BLACK(new Color(74, 70, 70)),
    WHITE(new Color(241, 235, 235));

    private final Color color;

    CardColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

}

