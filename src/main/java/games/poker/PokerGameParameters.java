package games.poker;

import core.AbstractParameters;
//import games.uno.UnoGameParameters;

import java.util.Arrays;
import java.util.Objects;

public class PokerGameParameters extends AbstractParameters {
    public String dataPath = "data/FrenchCards/";

    public double money = 0.00;
    public int nCardsPerPlayer = 2;
    public int nNumberCards = 10;
    public int QueenCards = 1;
    public int KingCards = 1;
    public int JackCards = 1;
    public int AceCards = 1;
    public int MaxCards = 7;
    public String[] suite = new String[]{
            "Diamonds",
            "Hearts",
            "Clubs",
            "Spades"
    };

    public PokerGameParameters(long seed) {
        super(seed);
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        PokerGameParameters pgp = new PokerGameParameters(System.currentTimeMillis());
        pgp.dataPath = dataPath;
        pgp.nCardsPerPlayer = nCardsPerPlayer;
        pgp.QueenCards = QueenCards;
        pgp.KingCards = KingCards;
        pgp.JackCards = JackCards;
        pgp.AceCards = AceCards;
        pgp.suite = suite.clone();
        return pgp;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PokerGameParameters)) return false;
        if (!super.equals(o)) return false;
        PokerGameParameters that = (PokerGameParameters) o;
        return nCardsPerPlayer == that.nCardsPerPlayer &&
                nNumberCards == that.nNumberCards &&
                QueenCards == that.QueenCards &&
                KingCards == that.KingCards &&
                JackCards == that.JackCards &&
                AceCards == that.AceCards &&
                Objects.equals(dataPath, that.dataPath) &&
                Arrays.equals(suite, that.suite);
    }
    /*
    @Override
    protected boolean _equals(Object o) {
        int result = Objects.hash(super.hashCode(), dataPath, nCardsPerPlayer, diamondNumberCards, spadeNumberCards, heartNumberCards, clubNumberCards, QueenCards, KingCards, JackCards, AceCards);

        return result;
    }
   */

}
