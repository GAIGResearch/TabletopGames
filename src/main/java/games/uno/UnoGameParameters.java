package games.uno;

import core.AbstractGameParameters;

public class UnoGameParameters extends AbstractGameParameters {
    public int nNumberCards = 10;
    public int nCardsPerPlayer = 7;
    public int nWildCards = 4;
    public int nSkipCards = 2;
    public int nReverseCards = 2;
    public int nDrawCards = 2;
    public int[] specialDrawCards = new int[]{2};  // DrawTwo card
    public int[] specialWildDrawCards = new int[]{0, 4};  // Wild, WildDrawFour card
    public String[] colors = new String[]{
            "Red",
            "Blue",
            "Green",
            "Yellow"
    };

    @Override
    protected AbstractGameParameters _copy() {
        return new UnoGameParameters();
    }
}
