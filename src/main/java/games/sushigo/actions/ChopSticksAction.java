package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;

public class ChopSticksAction extends AbstractAction {
    int playerId;

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
        return new ChopSticksAction(playerId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof DebugAction;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use ChopSticks";
    }
}

