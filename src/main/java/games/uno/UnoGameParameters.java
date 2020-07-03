package games.uno;

import core.AbstractGameParameters;

public class UnoGameParameters extends AbstractGameParameters {
    public String dataPath = "data/uno/";

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

    public int nReversePoints = 20;
    public int nDraw2Points = 20;
    public int nSkipPoints = 20;
    public int nWildPoints = 50;
    public int nWildDrawPoints = 50;
    public int nWinPoints = 500;

    public UnoGameParameters(long seed) {
        super(seed);
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractGameParameters _copy() {
        UnoGameParameters ugp = new UnoGameParameters(System.currentTimeMillis());
        ugp.dataPath = dataPath;
        ugp.nNumberCards = nNumberCards;
        ugp.nCardsPerPlayer = nCardsPerPlayer;
        ugp.nWildCards = nWildCards;
        ugp.nSkipCards = nSkipCards;
        ugp.nReverseCards = nReverseCards;
        ugp.nDrawCards = nDrawCards;
        ugp.specialDrawCards = specialDrawCards.clone();
        ugp.specialWildDrawCards = specialWildDrawCards.clone();
        ugp.colors = colors.clone();
        return ugp;
    }
}
