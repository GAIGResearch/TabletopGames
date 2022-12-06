package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sirius.*;

public class TakeAllCards extends AbstractAction {

    public final int location;

    public TakeAllCards(int location) {
        this.location = location;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        int player = state.getCurrentPlayer();
        Moon location = state.getMoon(state.getLocationIndex(player));
        switch (location.getMoonType()) {
            case MINING:
            case PROCESSING:
                state.getPlayerHand(player).add(location.getDeck());
                break;
            case METROPOLIS:
            case OUTPOST:
            case TRADING:
                throw new AssertionError("TakeAllCards not implemented for " + location.getMoonType());
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TakeAllCards;
    }

    @Override
    public int hashCode() {
        return 3908123;
    }

    @Override
    public String toString() {
        return "Takes All Cards from " + location;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        SiriusGameState state = (SiriusGameState) gameState;
        return String.format("Takes All Cards from %s", state.getMoon(location));
    }
}
