package games.diamant.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.diamant.DiamantGameState;


public class ContinueInCave extends AbstractAction implements IPrintable {
    // The player making the choice. All players choose at once, so the action has to carry this
    // itself rather than read it from the turn owner: inside a SimultaneousAction the turn owner is
    // whoever happened to hold the turn when the joint action was applied.
    public final int playerId;

    public ContinueInCave(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiamantGameState state = (DiamantGameState) gs;
        state.setActionPlayed(playerId, this);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof ContinueInCave other && other.playerId == playerId;
    }

    @Override
    public int hashCode() {
        return 0 + 31 * playerId;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Continue in cave";
    }

    @Override
    public String toString() {
        return "Continue in cave (P" + playerId + ")";
    }
}
