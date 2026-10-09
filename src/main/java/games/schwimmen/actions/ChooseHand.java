package games.schwimmen.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.schwimmen.SchwimmenGameState;

/**
 * The dealer keeps their hand, or takes the extra hand without having seen it.
 */
public class ChooseHand extends AbstractAction {

    public final boolean takeExtraHand;

    public ChooseHand(boolean takeExtraHand) {
        this.takeExtraHand = takeExtraHand;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SchwimmenGameState state = (SchwimmenGameState) gs;
        PartialObservableDeck<FrenchCard> hand = state.getPlayerHand(state.getCurrentPlayer());
        Deck<FrenchCard> extraHand = state.getExtraHand();
        if (takeExtraHand) {
            // the dealer's old hand is turned face up on the table, and the dealer alone sees the new one
            state.getTable().add(hand);
            hand.clear();
            hand.add(extraHand);
        } else {
            state.getTable().add(extraHand);
        }
        extraHand.clear();
        return true;
    }

    @Override
    public ChooseHand copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChooseHand other && other.takeExtraHand == takeExtraHand;
    }

    @Override
    public int hashCode() {
        return takeExtraHand ? 734101 : 734103;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return takeExtraHand ? "Take the extra hand" : "Keep hand";
    }

    @Override
    public String toString() {
        return takeExtraHand ? "Take the extra hand" : "Keep hand";
    }
}
