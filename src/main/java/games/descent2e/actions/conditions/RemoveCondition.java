package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.herofeats.HealAllInRange;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RemoveCondition extends DescentAction {

    // This is strictly for actions, abilities and items that remove conditions
    // Not for when figures automatically remove Diseased or Poisoned for passing their Attribute Tests
    int f;
    DescentTypes.DescentCondition condition;
    public RemoveCondition(int f, DescentTypes.DescentCondition condition) {
        super(Triggers.ACTION_POINT_SPEND);
        this.f = f;
        this.condition = condition;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.f);
        f.removeCondition(condition);
        f.setRemovedConditionThisTurn(true);
        return true;
    }

    @Override
    public RemoveCondition copy() {
        return new RemoveCondition(f, condition);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.f);
        return f.hasCondition(condition);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RemoveCondition) {
            RemoveCondition other = (RemoveCondition) obj;
            return f == other.f && condition == other.condition;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(f, condition);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(this.f);
        return "Remove " + condition.toString() + " from " + f.getName().replace("Hero: ", "");
    }
}
