package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

public class AftershockTest extends AttributeTest {

    public AftershockTest(int testingFigure, Figure.Attribute attribute, int sourceFigure) {
        super(testingFigure, attribute, sourceFigure);
        attributeTestName = "Aftershock (Willpower) Test: " + sourceFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String retVal = super.getString(gameState);
        return "Aftershock (Willpower) Test by" + retVal.split("Test by")[1];
    }

    @Override
    public String toString() {
        String retVal = super.toString();
        return "Aftershock (Willpower) Test by" + retVal.split("Test by")[1];
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
            //System.out.println("Passed Aftershock (Willpower) Test!");
        }
        else
        {
            //System.out.println("Failed Aftershock (Willpower) Test!");
            String test = "Aftershock of " + ((Figure) dgs.getComponentById(super.getSourceFigure())).getName();
            DescentHelper.forcedFatigue(dgs, f, test);
        }
    }

    @Override
    public AftershockTest _copy() {
        return new AftershockTest(testingFigure, attribute, sourceFigure);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof AftershockTest test) {
            return super.equals(test);
        } else {
            return false;
        }
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.getTestingFigure());

        // We can only make each attribute test once per turn - if we have already taken it, we can't make another attempt
        if (f.hasAttributeTest(this)) return false;

        // Aftershock only applies for attacks made adjacent to Lord Merick
        Figure merick = (Figure) dgs.getComponentById(super.getSourceFigure());
        return DescentHelper.checkAdjacent(dgs, f, merick);
    }

}