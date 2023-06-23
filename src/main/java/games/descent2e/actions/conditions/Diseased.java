package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;

import static games.descent2e.actions.Triggers.*;

public class Diseased extends DescentAction {

    public Diseased() {
        super(FORCED);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Disease Attribute Test";
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        Figure f = dgs.getActingFigure();

        // Diseased tests against Willpower
        Figure.Attribute attribute = Figure.Attribute.Willpower;
        boolean attributeTest = attributeTest(dgs, f.getAttributeValue(attribute));
        System.out.println((attribute));

        if (attributeTest) {
            f.removeCondition(DescentTypes.DescentCondition.Disease);
            System.out.println("Passed Disease Test!");
        }
        else {
            if (!f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) {
                f.getAttribute(Figure.Attribute.Fatigue).increment();
                System.out.println("Failed Disease Test!");
            }
        }

        f.addAttributeTest(DescentTypes.DescentCondition.Disease);

        return true;
    }

    private boolean attributeTest(DescentGameState dgs, int attribute)
    {
        dgs.setAttributeDicePool(DicePool.constructDicePool("GREY", "BLACK"));

        dgs.getAttributeDicePool().roll(dgs.getRandom());

        if (dgs.getAttributeDicePool().getShields() <= attribute)
        {
            return true;
        }

        else
        {
            return false;
        }
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        // We can only make one Diseased attribute test per turn - if we have already taken it, we can't make another attempt
        return f.hasCondition(DescentTypes.DescentCondition.Disease) && !f.hasAttributeTest(DescentTypes.DescentCondition.Disease) && f.getNActionsExecuted().isMinimum();
    }
}
