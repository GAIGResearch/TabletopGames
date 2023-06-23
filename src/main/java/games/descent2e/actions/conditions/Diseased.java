package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

public class Diseased extends AttributeTest {

    public Diseased(int testingFigure, Figure.Attribute attribute) {
        super(testingFigure, attribute);
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
    public void resolveTest(Figure f, boolean result)
    {
        if (result)
        {
            f.removeCondition(DescentTypes.DescentCondition.Disease);
            System.out.println("Passed Disease Test!");
        }
        else
        {
            if (!f.getAttribute(Figure.Attribute.Fatigue).isMaximum())
            {
                f.getAttribute(Figure.Attribute.Fatigue).increment();
                System.out.println("Failed Disease Test!");
            }
        }

        f.addAttributeTest(DescentTypes.DescentCondition.Disease);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        // We can only make one Diseased attribute test per turn - if we have already taken it, we can't make another attempt
        return f.hasCondition(DescentTypes.DescentCondition.Disease) && !f.hasAttributeTest(DescentTypes.DescentCondition.Disease) && f.getNActionsExecuted().isMinimum();
    }
}
