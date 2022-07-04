package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Hero;

import java.util.List;

public class UseCurseDoll extends DescentAction implements IExtendedSequence {

    public UseCurseDoll() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return null;
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
    public boolean execute(DescentGameState gs) {
        Hero hero = (Hero) gs.getActingFigure();
        hero.getConditions().
    }

    @Override
    public UseCurseDoll copy() {
        return null;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return false;
    }
}
