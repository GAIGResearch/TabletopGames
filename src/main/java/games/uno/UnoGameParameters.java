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

    public UnoGameParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractGameParameters _copy() {
        UnoGameParameters ugp = new UnoGameParameters(System.currentTimeMillis());
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
