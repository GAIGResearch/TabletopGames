package games.catan;

import java.awt.*;

public class CatanConstants {
    public final static Color[] PlayerColors = new Color[]{Color.RED, Color.YELLOW, Color.BLUE, Color.GREEN};
    public final static int HEX_SIDES = 6;
    public static Color getPlayerColor(int playerID) {
        if (playerID >= 0 && playerID < PlayerColors.length) return PlayerColors[playerID];
        return new Color(0,0,0);
    }
}
