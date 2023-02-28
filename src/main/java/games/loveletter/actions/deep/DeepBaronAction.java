package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.BaronAction;

import java.util.List;
import java.util.Objects;

/**
 * The Baron lets two players compare their hand card. The player with the lesser valued card is removed from the game.
 */
public class DeepBaronAction extends AbstractAction implements IExtendedSequence, IPrintable {
    final int playerID;
    private boolean executed;

    public DeepBaronAction(int playerID) {
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState llgs) {
        llgs.setActionInProgress(this);
        return true;
    }

    public String toString(){
        return "Baron (" + playerID + ")";
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
        if (!(o instanceof DeepBaronAction)) return false;
        DeepBaronAction that = (DeepBaronAction) o;
        return playerID == that.playerID && executed == that.executed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, executed);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return BaronAction.generateActions((LoveLetterGameState) state, playerID);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        if (action instanceof BaronAction) executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public DeepBaronAction copy() {
        DeepBaronAction copy = new DeepBaronAction(playerID);
        copy.executed = executed;
        return copy;
    }
}
