package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

public class WailTest extends AttributeTest {

    public WailTest(int testingFigure, Figure.Attribute attribute, int sourceFigure, int testCount) {
        super(testingFigure, attribute, sourceFigure, testCount);
        attributeTestName = "Wail (Willpower) Test: " + sourceFigure + "-" + testCount;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Wail (Willpower) Test (" + super.getTestCount() + ") by " + super.getSourceFigure() + " on " + super.getTestingFigure();
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
            //System.out.println("Passed Wail (Willpower) Test!");
        }
        else
        {
            //System.out.println("Failed Wail (Willpower) Test!");
            String test = "Wail of " + ((Figure) dgs.getComponentById(super.getSourceFigure())).getName();
            DescentHelper.forcedFatigue(dgs, f, test);
            DescentHelper.forcedFatigue(dgs, f, test);
        }
    }

    @Override
    public WailTest _copy() {
        return new WailTest(testingFigure, attribute, sourceFigure, testCount);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof WailTest test) {
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