package games.dominion.actions;
import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionGameState;

public abstract class DominionAction extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        if (state.actionsLeft() < 1)
            throw new AssertionError("Insufficient actions to play action card " + this.toString());
        state.changeActions(-1);
        return _execute(state);
    }

    abstract boolean _execute(DominionGameState state);
}
