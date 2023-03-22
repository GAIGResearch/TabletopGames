package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.interfaces.IExtendedSequence;
import games.catan.CatanActionFactory;
import games.catan.CatanGameState;

import java.util.List;
import java.util.Objects;

public class DiscardCardsPhase extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public final ActionSpace actionSpace;
    public final int nDiscards;

    private int nDiscarded;

    public DiscardCardsPhase(int playerID, ActionSpace actionSpace, int nDiscards) {
        this.playerID = playerID;
        this.actionSpace = actionSpace;
        this.nDiscards = nDiscards;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return CatanActionFactory.getDiscardActions((CatanGameState) state, actionSpace, playerID, nDiscards);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        nDiscarded++;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return actionSpace.structure == ActionSpace.Structure.Flat || actionSpace.structure == ActionSpace.Structure.Default || nDiscarded == nDiscards;
    }

    @Override
    public DiscardCardsPhase copy() {
        DiscardCardsPhase copy = new DiscardCardsPhase(playerID, actionSpace, nDiscards);
        copy.nDiscarded = nDiscarded;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscardCardsPhase)) return false;
        DiscardCardsPhase that = (DiscardCardsPhase) o;
        return playerID == that.playerID && nDiscards == that.nDiscards && nDiscarded == that.nDiscarded && Objects.equals(actionSpace, that.actionSpace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, actionSpace, nDiscards, nDiscarded);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Player " + playerID + " discards " + nDiscards;
    }
}
