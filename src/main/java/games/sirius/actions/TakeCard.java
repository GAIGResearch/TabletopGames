package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sirius.*;

public class TakeCard extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        int player = state.getCurrentPlayer();
        Moon currentLocation = state.getMoon(state.getLocationIndex(player));
        SiriusCard card = currentLocation.drawCard();
        if (card == null) {
            throw new AssertionError("No cards available at " + currentLocation);
        }
        state.addCardToHand(player, card);
        if (currentLocation.getMoonType() == SiriusConstants.MoonType.METROPOLIS)
            state.setActionTaken("Favour", player);
        return true;
    }

    @Override
    public TakeCard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TakeCard;
    }

    @Override
    public int hashCode() {
        return 2798423;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        SiriusGameState state = (SiriusGameState) gameState;
        int player = state.getCurrentPlayer();
        return "Take Card at " + state.getMoon(state.getLocationIndex(player));
    }

    @Override
    public String toString() {
        return "Take Card";
    }
}
