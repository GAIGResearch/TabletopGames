package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;

public class EndTurn extends DescentAction{
    public EndTurn() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "End turn";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        gs.getActingFigure().getNActionsExecuted().setToMax();
        gs.getTurnOrder().endPlayerTurn(gs);
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return true;
    }
}
