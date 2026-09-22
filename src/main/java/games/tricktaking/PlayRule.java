package games.tricktaking;

import core.components.FrenchCard;

import java.util.ArrayList;
import java.util.List;

/**
 * A game's rule for which cards in a hand may be played to the current trick. Games with restrictions on leading
 * (e.g. hearts or spades not yet broken) compose their own rule with these.
 */
@FunctionalInterface
public interface PlayRule {

    /**
     * The cards in the hand that may be played to the trick, in hand order.
     */
    List<FrenchCard> legalPlays(List<FrenchCard> hand, Trick trick);

    /**
     * Any card may be led; after that a player must follow the suit led if they can, and otherwise may play anything.
     * A card's suit is the one the trick's {@link CardOrder} gives it.
     */
    PlayRule FOLLOW_SUIT = (hand, trick) -> {
        FrenchCard.Suite lead = trick.getLeadSuit();
        CardOrder order = trick.getOrder();
        if (lead == null || hand.stream().noneMatch(c -> order.suitOf(c) == lead))
            return new ArrayList<>(hand);
        return hand.stream().filter(c -> order.suitOf(c) == lead).toList();
    };

    /**
     * As FOLLOW_SUIT, except that a card of the given suit may not be led unless the hand holds nothing else. Used for
     * a suit that may not be led until it is broken (Spades in Spades, Hearts in Hearts).
     */
    static PlayRule leadRestricted(FrenchCard.Suite suit) {
        return (hand, trick) -> {
            CardOrder order = trick.getOrder();
            if (trick.getSize() > 0 || hand.stream().allMatch(c -> order.suitOf(c) == suit))
                return FOLLOW_SUIT.legalPlays(hand, trick);
            return hand.stream().filter(c -> order.suitOf(c) != suit).toList();
        };
    }
}
