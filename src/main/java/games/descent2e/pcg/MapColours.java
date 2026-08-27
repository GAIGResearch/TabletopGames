package games.descent2e.pcg;

import games.chinesecheckers.components.Peg;

import java.awt.*;

public class MapColours {

    public enum Colours {
        Rainbow, Viridis, Monochrome
    }

    public static Color[] getColourStops (Colours colour) {
        Color[] retval = new Color[5];
        switch (colour) {
            case Rainbow -> {
                retval[0] = Color.BLUE;
                retval[1] = Color.CYAN;
                retval[2] = Color.GREEN;
                retval[3] = Color.YELLOW;
                retval[4] = Color.RED;
                break;
            }
            case Viridis -> {
                retval[0] = new Color(68, 1, 84);
                retval[1] = new Color(59, 82, 139);
                retval[2] = new Color(33, 145, 140);
                retval[3] = new Color(94, 201, 98);
                retval[4] = new Color(253, 231, 37);
                break;
            }

            default -> {
                retval[0] = new Color(0,0,0);
                retval[1] = new Color(64, 64, 64);
                retval[2] = new Color(128,128, 128);
                retval[3] = new Color(192, 192, 192);
                retval[4] = new Color(255, 255, 255);
                break;
            }
        }
        return retval;
    }
}
