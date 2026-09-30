package games.rummy;

import core.CoreConstants;
import core.components.Deck;
import core.components.FrenchCard;

/**
 * A meld on the table: a set of cards of one rank, or a run of one suit in rank order. Melds are equal when they
 * hold the same cards in the same order, so two states with the same melds are equal whatever their component IDs.
 */
public class RummyMeld extends Deck<FrenchCard> {

    public RummyMeld() {
        super("Meld", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
    }

    private RummyMeld(int componentID) {
        super("Meld", -1, componentID, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
    }

    @Override
    public RummyMeld copy() {
        RummyMeld copy = new RummyMeld(componentID);
        copyTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RummyMeld other && getComponents().equals(other.getComponents());
    }

    @Override
    public int hashCode() {
        return 610351 + getComponents().hashCode();
    }
}
