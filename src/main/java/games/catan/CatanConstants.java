package games.catan;

import java.awt.*;

public class CatanConstants {
    public final static Color[] PlayerColors = new Color[]{
            new Color(189, 11, 11),
            new Color(222, 178, 0),
            new Color(10, 143, 187),
            new Color(20, 162, 11),
            };
    public final static int HEX_SIDES = 6;
    public static Color getPlayerColor(int playerID) {
        if (playerID >= 0 && playerID < PlayerColors.length) return PlayerColors[playerID];
        return new Color(0,0,0);
    }
}
