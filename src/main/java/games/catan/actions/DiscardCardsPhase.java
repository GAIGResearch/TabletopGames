package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.catan.CatanActionFactory;
import games.catan.CatanGameState;

import java.util.List;

public class DiscardCardsPhase extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    public DiscardCardsPhase(int playerID) {
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return CatanActionFactory.getDiscardActions((CatanGameState) state);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return 0;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return false;
    }

    @Override
    public DiscardCardsPhase copy() {
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
