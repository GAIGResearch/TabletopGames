package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import java.util.Objects;

public class Diseased extends AttributeTest {

    String attributeTestName = "Diseased";

    public Diseased(int testingFigure, Figure.Attribute attribute) {
        super(testingFigure, attribute);
        super.setSourceFigure(testingFigure);
        super.setTestCount(0);
    }

    public void announceTestDebug (DescentGameState dgs)
    {
        System.out.println(((Figure) dgs.getComponentById(super.getTestingFigure())).getName() + " must make a Diseased Test!");
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
    public void resolveTest(DescentGameState dgs, Figure f, boolean result)
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

        f.addAttributeTest(this);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        // We can only make one Diseased attribute test per turn - if we have already taken it, we can't make another attempt
        return f.hasCondition(DescentTypes.DescentCondition.Disease) && !f.hasAttributeTest(this) && f.getNActionsExecuted().isMinimum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Diseased diseased = (Diseased) o;
        return Objects.equals(attributeTestName, diseased.attributeTestName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attributeTestName);
    }
}
