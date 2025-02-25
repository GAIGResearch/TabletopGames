package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;

import java.util.Objects;

public class RemoveCondition extends DescentAction {

    // This is strictly for actions, abilities and items that remove conditions
    // Not for when figures automatically remove Diseased or Poisoned for passing their Attribute Tests
    int figureID;
    DescentTypes.DescentCondition condition;
    public RemoveCondition(int figureID, DescentTypes.DescentCondition condition) {
        super(Triggers.ACTION_POINT_SPEND);
        this.figureID = figureID;
        this.condition = condition;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.figureID);
        f.removeCondition(condition);
        f.setRemovedConditionThisTurn(true);
        return true;
    }

    @Override
    public RemoveCondition copy() {
        return new RemoveCondition(figureID, condition);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.figureID);
        return f.hasCondition(condition);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RemoveCondition other) {
            return figureID == other.figureID && condition == other.condition;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), figureID, condition);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(this.figureID);
        return "Remove " + condition.toString() + " from " + f.getName().replace("Hero: ", "");
    }
}
