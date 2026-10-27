package games.scarto;

import core.components.TarotCard;
import games.tricktaking.PlayRule;

import java.util.ArrayList;
import java.util.List;

public final class ScartoUtils {

    private ScartoUtils() {
    }

    /**
     * The cards that may be played to the trick.
     */
    public static final PlayRule<TarotCard, TarotCard.Suit> PLAY_RULE = (hand, trick) -> {
        TarotCard.Suit lead = trick.getLeadSuit();
        // any card may be led, or played after a lone Fool
        if (lead == null)
            return new ArrayList<>(hand);
        // follow suit if possible, or else trump if possible, or else play anything; the Fool may always be played
        TarotCard.Suit must = holds(hand, lead) ? lead : holds(hand, TarotCard.Suit.Trumps) ? TarotCard.Suit.Trumps : null;
        if (must == null)
            return new ArrayList<>(hand);
        return hand.stream().filter(c -> c.isFool() || c.suit == must).toList();
    };

    /**
     * The cards in the dealer's hand that they may discard in the exchange.
     */
    public static List<TarotCard> discardable(List<TarotCard> hand) {
        // not a King, the Angel or the Fool; nor the Pagat, unless it is the only trump - the Fool counting as one
        boolean pagatAlone = hand.stream().filter(c -> c.isTrump() || c.isFool()).count() == 1;
        return hand.stream().filter(c -> !(c.isFool() || c.number == TarotCard.KING && !c.isTrump()
                || c.isTrump() && (c.number == TarotCard.ANGEL || c.number == TarotCard.PAGAT && !pagatAlone))).toList();
    }

    private static boolean holds(List<TarotCard> hand, TarotCard.Suit suit) {
        return hand.stream().anyMatch(c -> c.suit == suit);
    }
}
