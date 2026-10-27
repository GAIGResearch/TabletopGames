package games.rummy.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.rummy.RummyGameState;
import games.rummy.RummyMeld;
import games.rummy.RummyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adds a card from the hand to a meld on the table.
 */
public class LayOff extends AbstractAction {

    /**
     * Where the card goes. A card fits at most one set, and at most one run at each end, so this identifies the meld.
     */
    public enum Position {
        // onto the set of its rank
        SET,
        // below the lowest card of a run of its suit
        LOW,
        // above the highest card of a run of its suit
        HIGH
    }

    public final FrenchCard card;
    public final Position position;

    public LayOff(FrenchCard card, Position position) {
        this.card = card;
        this.position = position;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RummyGameState state = (RummyGameState) gs;
        RummyMeld meld = state.meldFor(card, position);
        state.getPlayerHand(state.getCurrentPlayer()).remove(card);
        switch (position) {
            case LOW -> meld.add(card);
            case HIGH -> meld.addToBottom(card);
            case SET -> {
                List<FrenchCard> cards = new ArrayList<>(meld.getComponents());
                cards.add(card);
                cards.sort(RummyUtils.MELD_ORDER);
                meld.setComponents(cards);
            }
        }
        return true;
    }

    @Override
    public LayOff copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LayOff other && Objects.equals(other.card, card) && other.position == position;
    }

    @Override
    public int hashCode() {
        return 610387 + 31 * Objects.hashCode(card) + 997 * position.ordinal();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Lay off " + card + (position == Position.SET ? " on its set" : position == Position.LOW ? " below a run" : " above a run");
    }
}
