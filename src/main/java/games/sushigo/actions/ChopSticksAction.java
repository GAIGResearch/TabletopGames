package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;

public class ChopSticksAction extends AbstractAction {
    final int playerId;

    public ChopSticksAction(int playerId)
    {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SGGameState SGGS = (SGGameState) gs;
        SGGS.setPlayerChopsticksActivated(playerId, true);
        SGGS.setPlayerExtraTurns(playerId, 2);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof ChopSticksAction) {
            return ((ChopSticksAction) obj).playerId == playerId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return playerId + 38923;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use ChopSticks";
    }
}

