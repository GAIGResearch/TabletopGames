package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

public class Poisoned extends AttributeTest {

    public Poisoned(int testingFigure, Figure.Attribute attribute) {
        super(testingFigure, attribute);
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
    public void resolveTest(Figure f, boolean result)
    {
        if (result)
        {
            f.removeCondition(DescentTypes.DescentCondition.Poison);
            System.out.println("Passed Poisoned Test!");
        }
        else
        {
            if (!f.getAttribute(Figure.Attribute.Health).isMinimum())
            {
                f.getAttribute(Figure.Attribute.Health).decrement();
                System.out.println("Failed Poisoned Test!");
            }
        }

        f.addAttributeTest(DescentTypes.DescentCondition.Poison);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        // We can only make one Poisoned attribute test per turn - if we have already taken it, we can't make another attempt
        return f.hasCondition(DescentTypes.DescentCondition.Poison) && !f.hasAttributeTest(DescentTypes.DescentCondition.Poison) && f.getNActionsExecuted().isMinimum();
    }
}
