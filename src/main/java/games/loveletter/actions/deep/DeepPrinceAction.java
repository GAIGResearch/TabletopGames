package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.PrinceAction;

import java.util.List;
import java.util.Objects;

/**
 * The targeted player discards its current and draws a new one.
 * In case the discarded card is a princess, the targeted player is removed from the game.
 */
public class DeepPrinceAction extends AbstractAction implements IExtendedSequence, IPrintable {
    private final int playerId;
    private boolean executed;

    public DeepPrinceAction(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState llgs) {
        llgs.setActionInProgress(this);
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeepPrinceAction)) return false;
        DeepPrinceAction that = (DeepPrinceAction) o;
        return playerId == that.playerId && executed == that.executed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, executed);
    }

    @Override
    public String toString(){
        return "Prince (" + playerId + ")";
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
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return PrinceAction.generateActions((LoveLetterGameState) state, playerId);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerId;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        if (action instanceof PrinceAction) executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public DeepPrinceAction copy() {
        DeepPrinceAction pa = new DeepPrinceAction(playerId);
        pa.executed = executed;
        return pa;
    }

}
