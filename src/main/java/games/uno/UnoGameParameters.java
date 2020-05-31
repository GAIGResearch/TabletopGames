package games.uno;

import core.AbstractGameParameters;

public class UnoGameParameters extends AbstractGameParameters {
    public int nNumberCards = 10;
    public int nCardsPerPlayer = 7;
    public int nWildCards = 4;
    public int nSkipCards = 2;
    public int nReverseCards = 2;
    public int nDrawCards = 2;
    public String[] colors = new String[]{
            "Red",
            "Blue",
            "Green",
            "Yellow",
            "Wild"
    };
}
