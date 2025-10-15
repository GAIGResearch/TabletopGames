package games.gofish.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class GoFishEndTurnAction extends AbstractAction {

    private final int playerId;

    public GoFishEndTurnAction(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // No state change here; ForwardModel will advance the turn.
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new GoFishEndTurnAction(playerId);
    }

    @Override
    public int hashCode() {
        return 31 * playerId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GoFishEndTurnAction)) return false;
        return ((GoFishEndTurnAction) obj).playerId == this.playerId;
    }

    @Override
    public String toString() {
        return "Pass / End Turn (P" + playerId + ")";
    }


    @Override
    public String getString(AbstractGameState gameState) {
        return toString();  // reuse toString for logging/GUI
    }
}
