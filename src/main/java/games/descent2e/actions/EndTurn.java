package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;

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
        Figure f = gs.getActingFigure();
        f.getNActionsExecuted().setToMax();

        // Removes all attribute tests taken this turn from the list, so we can check them again next turn
        f.clearAttributeTest();

        // If we are Immobilized, remove that condition now
        if(f.hasCondition(DescentTypes.DescentCondition.Immobilize)) { f.removeCondition(DescentTypes.DescentCondition.Immobilize); }

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
