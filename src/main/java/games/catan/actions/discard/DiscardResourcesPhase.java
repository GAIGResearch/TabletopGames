package games.catan.actions.discard;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.catan.CatanActionFactory;
import games.catan.CatanGameState;

import java.util.List;
import java.util.Objects;

/**
 * Combinations of resources in hand to discard.
 */
public class DiscardResourcesPhase extends AbstractAction implements IExtendedSequence {
    public final int playerID;  // player discarding
    public final int nDiscards;  // how many resources to discard

    private int nDiscarded;  // how many discarded so far

    public DiscardResourcesPhase(int playerID, int nDiscards) {
        this.playerID = playerID;
        this.nDiscards = nDiscards;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (nDiscards > 0) {
            gs.setActionInProgress(this);
        }
        return true;
    }

    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return CatanActionFactory.getDiscardActions((CatanGameState) state, state.getCoreGameParameters().actionSpace, playerID, nDiscards);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state, ActionSpace actionSpace) {
        return CatanActionFactory.getDiscardActions((CatanGameState) state, actionSpace, playerID, nDiscards);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof DoNothing) nDiscarded = nDiscards;
        else if (action instanceof  DiscardResources) {
            nDiscarded += ((DiscardResources) action).resourcesToDiscard.length;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return nDiscarded == nDiscards;
    }

    @Override
    public DiscardResourcesPhase copy() {
        DiscardResourcesPhase copy = new DiscardResourcesPhase(playerID, nDiscards);
        copy.nDiscarded = nDiscarded;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscardResourcesPhase)) return false;
        DiscardResourcesPhase that = (DiscardResourcesPhase) o;
        return playerID == that.playerID && nDiscards == that.nDiscards && nDiscarded == that.nDiscarded;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, nDiscards, nDiscarded);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "p" + playerID + " discards " + nDiscards + " resources";
    }
}
