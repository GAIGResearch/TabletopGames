package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

public class WailTest extends AttributeTest {

    public WailTest(int testingFigure, Figure.Attribute attribute, int sourceFigure) {
        super(testingFigure, attribute, sourceFigure);
        attributeTestName = "Wail (Willpower) Test: " + sourceFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String retVal = super.getString(gameState);
        return "Wail (Willpower) Test by" + retVal.split("Test by")[1];
    }

    @Override
    public String toString() {
        String retVal = super.toString();
        return "Wail (Willpower) Test by" + retVal.split("Test by")[1];
    }

    @Override
    public String toStringWithResult() {
        return this + " - " + getTestingName() + " (" + (result ? "Passed)" : "Failed; +2 Fatigue)");
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
        return new WailTest(testingFigure, attribute, sourceFigure);
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
}