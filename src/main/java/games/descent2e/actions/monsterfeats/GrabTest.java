package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import static games.descent2e.DescentHelper.immobilize;

public class GrabTest extends AttributeTest {

    public GrabTest(int testingFigure, Figure.Attribute attribute, int sourceFigure) {
        super(testingFigure, attribute, sourceFigure);
        attributeTestName = "Grab (Might) Test: " + sourceFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String retVal = super.getString(gameState);
        return "Grab (Might) Test by" + retVal.split("Test by")[1];
    }

    @Override
    public String toString() {
        String retVal = super.toString();
        return "Grab (Might) Test by" + retVal.split("Test by")[1];
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
            //System.out.println("Passed Grab (Might) Test!");
        }
        else
        {
            //System.out.println("Failed Grab (Might) Test!");
            immobilize(f);
        }
    }

    @Override
    public GrabTest _copy() {
        return new GrabTest(testingFigure, attribute, sourceFigure);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof GrabTest test) {
            return super.equals(test);
        } else {
            return false;
        }
    }
}