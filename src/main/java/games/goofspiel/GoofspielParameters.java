package games.goofspiel;

import core.AbstractParameters;
import core.components.FrenchCard;
import evaluation.optimisation.TunableParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.components.FrenchCard.FrenchCardType.*;
import static core.components.FrenchCard.Suite.*;

/**
 * Parameters for Goofspiel (GOPS). The defaults are Pagat's rules; the alternatives are Pagat's variants.
 */
public class GoofspielParameters extends TunableParameters<GoofspielParameters> {

    /**
     * What happens to the prize cards on offer when the highest bid is tied.
     */
    public enum TieRule {
        /** The prizes stay on offer and are contested with the next prize. */
        CARRY_OVER,
        /** Tied bids are disqualified and the highest remaining bid wins. If no bid is unique the prizes are discarded. */
        HIGHEST_UNIQUE,
        /** The prizes are discarded. */
        DISCARD
    }

    /** The suit of the prize deck. */
    public static final FrenchCard.Suite PRIZE_SUIT = Diamonds;
    /** The suits dealt to the players, in seat order; with more than three players they repeat (a second pack). */
    public static final List<FrenchCard.Suite> HAND_SUITS = List.of(Clubs, Spades, Hearts);

    public int cardsPerSuit = 13;
    public boolean aceHigh = false;
    public TieRule tieRule = TieRule.CARRY_OVER;

    public GoofspielParameters() {
        addTunableParameter("cardsPerSuit", 13, Arrays.asList(5, 7, 9, 11, 13));
        addTunableParameter("aceHigh", false, Arrays.asList(false, true));
        addTunableParameter("tieRule", TieRule.CARRY_OVER, Arrays.asList(TieRule.values()));
    }

    @Override
    public void _reset() {
        cardsPerSuit = (int) getParameterValue("cardsPerSuit");
        aceHigh = (boolean) getParameterValue("aceHigh");
        tieRule = (TieRule) getParameterValue("tieRule");
    }

    /**
     * The value of a card as a bid and as a prize. Ace 1 (14 if aceHigh), Jack 11, Queen 12, King 13.
     */
    public int cardValue(FrenchCard card) {
        // FrenchCard numbers the Ace as 14
        if (card.type == Ace)
            return aceHigh ? 14 : 1;
        return card.number;
    }

    /**
     * The cards of one suit used in the game, the cardsPerSuit lowest-valued ranks in ascending order.
     */
    public List<FrenchCard> suitCards(FrenchCard.Suite suit) {
        List<FrenchCard> cards = new ArrayList<>();
        int lowest = aceHigh ? 2 : 1;
        for (int value = lowest; value < lowest + cardsPerSuit; value++) {
            cards.add(switch (value) {
                case 1, 14 -> new FrenchCard(Ace, suit);
                case 11 -> new FrenchCard(Jack, suit);
                case 12 -> new FrenchCard(Queen, suit);
                case 13 -> new FrenchCard(King, suit);
                default -> new FrenchCard(FrenchCard.FrenchCardType.Number, suit, value);
            });
        }
        return cards;
    }

    public FrenchCard.Suite handSuit(int player) {
        return HAND_SUITS.get(player % HAND_SUITS.size());
    }

    @Override
    protected AbstractParameters _copy() {
        return new GoofspielParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof GoofspielParameters;
    }

    @Override
    public GoofspielParameters instantiate() {
        return this;
    }
}
