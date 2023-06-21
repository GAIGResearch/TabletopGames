package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;

import static games.descent2e.actions.Triggers.ACTION_POINT_SPEND;

public class Poisoned extends DescentAction {

    public Poisoned() {
        super(ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Poison Attribute Test";
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        Figure f = dgs.getActingFigure();

        // TODO Attribute Test

        boolean attributeTest = true;

        int might = f.getAttributeValue(Figure.Attribute.Might);

        dgs.setAttributeDicePool(DicePool.constructDicePool("GREY", "BLACK"));

        dgs.getAttributeDicePool().roll(dgs.getRandom());
        int result = dgs.getAttributeDicePool().getShields();

        if (result > might)
        {
            // If result <= might, success; else, failure
            attributeTest = false;
        }

        if (attributeTest) {
            f.removeCondition(DescentTypes.DescentCondition.Poison);
        }
        else {
            if (!f.getAttribute(Figure.Attribute.Health).isMinimum()) {
                f.getAttribute(Figure.Attribute.Health).decrement();
            }
        }

        f.addAttributeTest(DescentTypes.DescentCondition.Poison);

        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        // We can only make one Poisoned attribute test per turn - if we have already taken it, we can't make another attempt
        return f.hasCondition(DescentTypes.DescentCondition.Poison) && !f.hasAttributeTest(DescentTypes.DescentCondition.Poison) && f.getNActionsExecuted().isMinimum();
    }
}
