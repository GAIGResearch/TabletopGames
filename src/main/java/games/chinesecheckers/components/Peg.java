package games.chinesecheckers.components;

import java.awt.*;

/**
 * A peg of a player's colour, and whether it has reached the destination of that colour (after which it may not
 * leave it).
 */
public record Peg(Colour colour, boolean inDestination) {

    public enum Colour {
        purple,
        blue,
        yellow,
        red,
        orange,
        green,
        neutral;

        public Color toGraphicsColor() {
            return switch (this) {
                case purple -> Color.magenta;
                case blue -> Color.blue;
                case yellow -> Color.yellow;
                case red -> Color.red;
                case orange -> Color.orange;
                case green -> Color.green;
                case neutral -> Color.black;
            };
        }
    }
}
