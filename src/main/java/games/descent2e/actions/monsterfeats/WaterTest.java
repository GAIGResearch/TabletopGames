package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

public class WaterTest extends AttributeTest {

    public WaterTest(int testingFigure, Figure.Attribute attribute, int sourceFigure, int testCount) {
        super(testingFigure, attribute, sourceFigure, testCount);
        attributeTestName = "Water (Willpower) Test: " + sourceFigure + "-" + testCount;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Water (Willpower) Test (" + super.getTestCount() + ") by " + super.getSourceFigure() + " on " + super.getTestingFigure();
    }

    @Override
    public String toStringWithResult() {
        return this + " - " + getTestingName() + " (" + (result ? "Passed)" : "Failed; +1 Fatigue)");
    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            //System.out.println("Passed Water (Willpower) Test!");
        }
        else
        {
            //System.out.println("Failed Water (Willpower) Test!");
            String test = "Water of " + ((Figure) dgs.getComponentById(super.getSourceFigure())).getName();
            // Suffer 2 Fatigue on failure
            DescentHelper.forcedFatigue(dgs, f, test);
            DescentHelper.forcedFatigue(dgs, f, test);
        }
    }

    @Override
    public WaterTest _copy() {
        return new WaterTest(testingFigure, attribute, sourceFigure, testCount);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof WaterTest test) {
            return super.equals(test);
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