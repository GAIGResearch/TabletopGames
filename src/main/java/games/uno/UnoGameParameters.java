package games.uno;

import core.AbstractParameters;

import java.util.Arrays;
import java.util.Objects;


public class UnoGameParameters extends AbstractParameters {
    public String dataPath = "data/uno/";

    public int nCardsPerPlayer = 7;

    public int nNumberCards = 10;
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
    protected AbstractParameters _copy() {
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

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnoGameParameters)) return false;
        if (!super.equals(o)) return false;
        UnoGameParameters that = (UnoGameParameters) o;
        return nCardsPerPlayer == that.nCardsPerPlayer &&
                nNumberCards == that.nNumberCards &&
                nWildCards == that.nWildCards &&
                nSkipCards == that.nSkipCards &&
                nReverseCards == that.nReverseCards &&
                nDrawCards == that.nDrawCards &&
                nReversePoints == that.nReversePoints &&
                nDraw2Points == that.nDraw2Points &&
                nSkipPoints == that.nSkipPoints &&
                nWildPoints == that.nWildPoints &&
                nWildDrawPoints == that.nWildDrawPoints &&
                nWinPoints == that.nWinPoints &&
                Objects.equals(dataPath, that.dataPath) &&
                Arrays.equals(specialDrawCards, that.specialDrawCards) &&
                Arrays.equals(specialWildDrawCards, that.specialWildDrawCards) &&
                Arrays.equals(colors, that.colors);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), dataPath, nCardsPerPlayer, nNumberCards, nWildCards, nSkipCards, nReverseCards, nDrawCards, nReversePoints, nDraw2Points, nSkipPoints, nWildPoints, nWildDrawPoints, nWinPoints);
        result = 31 * result + Arrays.hashCode(specialDrawCards);
        result = 31 * result + Arrays.hashCode(specialWildDrawCards);
        result = 31 * result + Arrays.hashCode(colors);
        return result;
    }
}
