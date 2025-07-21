package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import static games.descent2e.DescentHelper.immobilize;

public class EarthTest extends AttributeTest {

    public EarthTest(int testingFigure, Figure.Attribute attribute, int sourceFigure, int testCount) {
        super(testingFigure, attribute, sourceFigure, testCount);
        attributeTestName = "Earth (Awareness) Test: " + sourceFigure + "-" + testCount;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Earth (Awareness) Test (" + super.getTestCount() + ") by " + super.getSourceFigure() + " on " + super.getTestingFigure();
    }

    @Override
    public String toStringWithResult() {
        return this + " - " + getTestingName() + " (" + (result ? "Passed)" : "Failed; Immobilized)");
    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            //System.out.println("Passed Earth (Awareness) Test!");
        }
        else
        {
            //System.out.println("Failed Earth (Awareness) Test!");
            immobilize(f);
        }
    }

    @Override
    public EarthTest _copy() {
        return new EarthTest(testingFigure, attribute, sourceFigure, testCount);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof EarthTest ht) {
            return super.equals(ht);
        } else {
            return false;
        }
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.getTestingFigure());

        // We can only make each attribute test once per turn - if we have already taken it, we can't make another attempt
        return !f.hasAttributeTest(this);
    }

}