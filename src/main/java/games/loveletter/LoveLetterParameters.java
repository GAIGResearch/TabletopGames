package games.loveletter;

import core.AbstractParameters;
import games.loveletter.cards.LoveLetterCard;

import java.util.HashMap;

/** This class allows to modify the balance of the game. Each modification may result in several problems with the
 * game's logic. Please be very careful in modifying these values and check for possible consequences.
 */
public class LoveLetterParameters extends AbstractParameters {

    // Occurrence count for each card
    HashMap<LoveLetterCard.CardType, Integer> cardCounts = new HashMap<LoveLetterCard.CardType, Integer>() {{
        put(LoveLetterCard.CardType.Princess, 1);
        put(LoveLetterCard.CardType.Countess, 1);
        put(LoveLetterCard.CardType.King, 1);
        put(LoveLetterCard.CardType.Prince, 2);
        put(LoveLetterCard.CardType.Handmaid, 2);
        put(LoveLetterCard.CardType.Baron, 2);
        put(LoveLetterCard.CardType.Priest, 2);
        put(LoveLetterCard.CardType.Guard, 5);
    }};

    // How many cards each player draws
    public int nCardsPerPlayer = 1;
    // How many cards are put on the side (visible) in minimum player games
    public int nCardsVisibleReserve = 3;
    // Affection tokens required to win, maps to number of players (2, 3, 4+)
    public int[] nTokensWin = new int[]{7, 5, 4};

    public LoveLetterParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        LoveLetterParameters llp = new LoveLetterParameters(System.currentTimeMillis());
        llp.cardCounts = new HashMap<>(cardCounts);
        llp.nCardsPerPlayer = nCardsPerPlayer;
        return llp;
    }
}
