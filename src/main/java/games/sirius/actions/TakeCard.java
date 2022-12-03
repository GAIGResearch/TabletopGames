package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sirius.*;

public class TakeCard extends AbstractAction {

    public final int valueTaken;

    public TakeCard(int value) {
        valueTaken = value;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        int player = state.getCurrentPlayer();
        Moon currentLocation = state.getMoon(state.getLocationIndex(player));
        SiriusCard card = currentLocation.drawCard(c -> c.value == valueTaken)
                .orElseThrow(() -> new AssertionError("No card with that value found : " + valueTaken));
        state.addCardToHand(player, card);
        currentLocation.lookAtDeck(player); // and they know what the other cards are
        return true;
    }

    @Override
    public TakeCard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TakeCard) {
            return valueTaken == ((TakeCard) obj).valueTaken;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return valueTaken + 2798423;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
    @Override
    public String toString() {
        return "Take Card of value " + valueTaken;
    }
}
