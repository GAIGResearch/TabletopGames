package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import static games.descent2e.DescentHelper.immobilize;

public class ThrowTest extends AttributeTest {

    public ThrowTest(int testingFigure, Figure.Attribute attribute, int sourceFigure) {
        super(testingFigure, attribute, sourceFigure);
        attributeTestName = "Throw (Might) Test: " + sourceFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String retVal = super.getString(gameState);
        return "Throw (Might) Test by" + retVal.split("Test by")[1];
    }

    @Override
    public String toString() {
        String retVal = super.toString();
        return "Throw (Might) Test by" + retVal.split("Test by")[1];
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
            System.out.println("Passed Throw (Might) Test!");
        }
        else
        {
            System.out.println("Failed Throw (Might) Test!");
            f.setOffMap(true);
            f.getAttribute(Figure.Attribute.MovePoints).setValue(3);
        }
    }

    @Override
    public ThrowTest _copy() {
        return new ThrowTest(testingFigure, attribute, sourceFigure);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ThrowTest test) {
            return super.equals(test);
        } else {
            return false;
        }
    }
}