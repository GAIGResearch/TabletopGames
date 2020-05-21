package games.loveletter;

import core.GameParameters;
import games.loveletter.cards.LoveLetterCard;

import java.util.HashMap;

public class LoveLetterParameters extends GameParameters {

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

    public int nCardsPerPlayer = 1;
}
