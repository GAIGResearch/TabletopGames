package games.blackjack;

import core.AbstractParameters;
import java.util.Arrays;

public class BlackjackParameters extends AbstractParameters {
    public String dataPath = "data/FrenchCards/";

    public int deckCards = 52;
    public int totalNumberCards = 10;
    public int nCardsPerPlayer = 2;
    public int aceCard = 1;
    public int jackCard = 10;
    public int queenCard = 10;
    public int kingCard = 10;
    public int blackJack = 21;
    public int bust = 22;
    public int winScore = 21;

    public boolean push = false;

    public int[] numberCards = {2,3,4,5,6,7,8,9,10};

    public String[] suite = new String[]{
            "Spades",
            "Clubs",
            "Hearts",
            "Diamonds"
    };


    public BlackjackParameters(long seed) {
        super(seed);
    }

    public String getDataPath(){
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        BlackjackParameters bjgp = new BlackjackParameters(System.currentTimeMillis());
        bjgp.dataPath = dataPath;
        bjgp.totalNumberCards = totalNumberCards;
        bjgp.deckCards = deckCards;
        bjgp.nCardsPerPlayer = nCardsPerPlayer;
        bjgp.aceCard = aceCard;
        bjgp.jackCard = jackCard;
        bjgp.queenCard = queenCard;
        bjgp.kingCard = kingCard;
        bjgp.push = push;
        bjgp.numberCards = numberCards;
        bjgp.suite = suite;
        return bjgp;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlackjackParameters)) return false;
        if (!super.equals(o)) return false;
        BlackjackParameters that = (BlackjackParameters) o;
        return deckCards == that.deckCards &&
                nCardsPerPlayer == that.nCardsPerPlayer &&
                aceCard == that.aceCard &&
                jackCard == that.jackCard &&
                queenCard == that.queenCard &&
                kingCard == that.kingCard &&
                blackJack == that.blackJack &&
                bust == that.bust &&
                winScore == that.winScore &&
                push == that.push &&
                Arrays.equals(numberCards, that.numberCards) &&
                Arrays.equals(suite, that.suite);
    }
}
