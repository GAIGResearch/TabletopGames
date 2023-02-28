package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.PriestAction;
import java.util.List;
import java.util.Objects;

/**
 * The Priest allows a player to see another player's hand cards.
 * This has no effect in case the game is fully observable.
 */
public class DeepPriestAction extends AbstractAction implements IExtendedSequence,IPrintable {

    private final int playerID;
    private boolean executed;

    public DeepPriestAction(int playerID) {
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState llgs) {
        llgs.setActionInProgress(this);
        return true;
    }

    public String toString(){
        return "Priest (" + playerID + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeepPriestAction)) return false;
        DeepPriestAction that = (DeepPriestAction) o;
        return playerID == that.playerID && executed == that.executed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, executed);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return PriestAction.generateActions((LoveLetterGameState) state, playerID);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        if (action instanceof PriestAction) executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public DeepPriestAction copy() {
        DeepPriestAction copy = new DeepPriestAction(playerID);
        copy.executed = executed;
        return copy;
    }
}
