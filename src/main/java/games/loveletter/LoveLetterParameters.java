package games.loveletter;

import core.AbstractGameParameters;
import games.loveletter.cards.LoveLetterCard;

import java.util.HashMap;

/** This class allows to modify the balance of the game. Each modification may result in several problems with the
 * game's logic. Please be very careful in modifying these values and check for possible consequences.
 */
public class LoveLetterParameters extends AbstractGameParameters {

    // set the occurrence count for each card
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

    // set how many cards each player draws
    public int nCardsPerPlayer = 1;

    @Override
    protected AbstractGameParameters _copy() {
        return new LoveLetterParameters();
    }
}
