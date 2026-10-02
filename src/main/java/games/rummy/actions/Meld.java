package games.rummy.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.rummy.RummyGameState;
import games.rummy.RummyMeld;
import games.rummy.RummyUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lays down a set or a run from the hand as a new meld on the table.
 */
public class Meld extends AbstractAction {

    // in meld order (RummyUtils.MELD_ORDER), whatever order they were given in
    public final List<FrenchCard> cards;

    public Meld(List<FrenchCard> cards) {
        List<FrenchCard> sorted = new ArrayList<>(cards);
        sorted.sort(RummyUtils.MELD_ORDER);
        this.cards = Collections.unmodifiableList(sorted);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RummyGameState state = (RummyGameState) gs;
        Deck<FrenchCard> hand = state.getPlayerHand(state.getCurrentPlayer());
        RummyMeld meld = new RummyMeld();
        for (FrenchCard card : cards) {
            hand.remove(card);
            meld.addToBottom(card);
        }
        state.getMelds().add(meld);
        state.setMeldedThisTurn(true);
        return true;
    }

    @Override
    public Meld copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Meld other && other.cards.equals(cards);
    }

    @Override
    public int hashCode() {
        return 610379 + 31 * cards.hashCode();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Meld " + cards;
    }
}
