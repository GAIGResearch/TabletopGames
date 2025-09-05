package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import java.util.Objects;

public class Diseased extends AttributeTest {

    public Diseased(int testingFigure, Figure.Attribute attribute) {
        super(testingFigure, attribute, testingFigure);
        attributeTestName = "Diseased Test: " + testingFigure;
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
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            f.removeCondition(DescentTypes.DescentCondition.Disease);
            f.addActionTaken("Passed Disease Test");
            //System.out.println("Passed Disease Test!");
        }
        else
        {
            //System.out.println("Failed Disease Test!");
            f.addActionTaken("Failed Disease Test");
            DescentHelper.forcedFatigue(dgs, f, "Disease");
        }
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        // We can only make one Diseased attribute test per turn - if we have already taken it, we can't make another attempt
        return f.hasCondition(DescentTypes.DescentCondition.Disease) && !f.hasAttributeTest(this) && f.getNActionsExecuted().isMinimum();
    }

    @Override
    public Diseased _copy() {
        return new Diseased(testingFigure, attribute);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof Diseased;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 28091;
    }
}
