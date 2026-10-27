package games.cribbage.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.cribbage.CribbageGameState;

import java.util.Comparator;
import java.util.Objects;

/**
 * Discard two cards from the current player's hand into the crib. Each card is visible only to the player who
 * discarded it. The cards are held in a fixed order, so DiscardToCrib(a, b) equals DiscardToCrib(b, a).
 */
public class DiscardToCrib extends AbstractAction {

    private static final Comparator<FrenchCard> ORDER =
            Comparator.comparing((FrenchCard c) -> c.suite).thenComparingInt(c -> c.number);

    public final FrenchCard first;
    public final FrenchCard second;

    public DiscardToCrib(FrenchCard a, FrenchCard b) {
        if (ORDER.compare(a, b) <= 0) {
            first = a;
            second = b;
        } else {
            first = b;
            second = a;
        }
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CribbageGameState state = (CribbageGameState) gs;
        int player = state.getCurrentPlayer();
        boolean[] visibility = new boolean[state.getNPlayers()];
        visibility[player] = true;
        for (FrenchCard card : new FrenchCard[]{first, second}) {
            state.getPlayerHand(player).remove(card);  // throws if the card is not in the hand
            state.getCrib().add(card, visibility);
        }
        return true;
    }

    @Override
    public DiscardToCrib copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscardToCrib that)) return false;
        return first.equals(that.first) && second.equals(that.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second) + 590317;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Discard " + first + " and " + second + " to crib";
    }
}
