package games.tricktaking;

import core.components.Component;
import core.components.FrenchCard;

import java.util.ArrayList;
import java.util.List;

/**
 * A game's rule for which cards in a hand may be played to the current trick. Games with restrictions on leading
 * (e.g. hearts or spades not yet broken) compose their own rule with these.
 *
 * @param <C> the type of card
 * @param <S> the type of suit
 */
@FunctionalInterface
public interface PlayRule<C extends Component, S> {

    /**
     * The cards in the hand that may be played to the trick, in hand order.
     */
    List<C> legalPlays(List<C> hand, Trick<C, S> trick);

    /**
     * Any card may be led; after that a player must follow the suit led if they can, and otherwise may play anything.
     * A card's suit is the one the trick's {@link CardOrder} gives it.
     */
    static <C extends Component, S> PlayRule<C, S> followSuit() {
        return (hand, trick) -> {
            S lead = trick.getLeadSuit();
            CardOrder<C, S> order = trick.getOrder();
            if (lead == null || hand.stream().noneMatch(c -> order.suitOf(c) == lead))
                return new ArrayList<>(hand);
            return hand.stream().filter(c -> order.suitOf(c) == lead).toList();
        };
    }

    /**
     * {@link #followSuit()} for French cards.
     */
    PlayRule<FrenchCard, FrenchCard.Suite> FOLLOW_SUIT = followSuit();

    /**
     * As {@link #followSuit()}, except that a card of the given suit may not be led unless the hand holds nothing
     * else. Used for a suit that may not be led until it is broken (Spades in Spades, Hearts in Hearts).
     */
    static <C extends Component, S> PlayRule<C, S> leadRestricted(S suit) {
        PlayRule<C, S> follow = followSuit();
        return (hand, trick) -> {
            CardOrder<C, S> order = trick.getOrder();
            if (trick.getSize() > 0 || hand.stream().allMatch(c -> order.suitOf(c) == suit))
                return follow.legalPlays(hand, trick);
            return hand.stream().filter(c -> order.suitOf(c) != suit).toList();
        };
    }
}
