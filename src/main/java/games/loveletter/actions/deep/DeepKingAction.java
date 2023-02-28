package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.KingAction;

import java.util.List;
import java.util.Objects;

/**
 * The King lets two players swap their hand cards.
 */
public class DeepKingAction extends AbstractAction implements IExtendedSequence, IPrintable {
    private final int playerID;
    private boolean executed;

    public DeepKingAction(int playerID) {
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState llgs) {
        llgs.setActionInProgress(this);
        return true;
    }

    public String toString(){
        return "King (" + playerID + ")";
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
        if (!(o instanceof DeepKingAction)) return false;
        DeepKingAction that = (DeepKingAction) o;
        return playerID == that.playerID && executed == that.executed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, executed);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return KingAction.generateActions((LoveLetterGameState) state, playerID);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        if (action instanceof KingAction) executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public DeepKingAction copy() {
        DeepKingAction copy = new DeepKingAction(playerID);
        copy.executed = executed;
        return copy;
    }

}
