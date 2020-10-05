package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class Trade extends AbstractAction {
    @Override
    public boolean execute(AbstractGameState gs) {
        // todo (mb) make trade offer and set the game phase to TradeReaction and make sure intended player has to react
        return false;
    }

    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }
}
