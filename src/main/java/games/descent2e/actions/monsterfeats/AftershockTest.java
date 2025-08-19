package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

public class AftershockTest extends AttributeTest {

    public AftershockTest(int testingFigure, Figure.Attribute attribute, int sourceFigure, int testCount) {
        super(testingFigure, attribute, sourceFigure, testCount);
        attributeTestName = "Aftershock (Willpower) Test: " + sourceFigure + "-" + testCount;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(this.getTestingFigure());
        Figure source = (Figure) gameState.getComponentById(this.getSourceFigure());

        String testingName = f.getName().replace("Hero: ", "");
        String sourceName = source.getName().replace("Hero: ", "");

        return "Aftershock (Willpower) Test (" + super.getTestCount() + ") by " + sourceName + " on " + testingName;
    }

    @Override
    public String toString() {
        return "Aftershock (Willpower) Test (" + super.getTestCount() + ") by " + super.getSourceFigure() + " on " + super.getTestingFigure();
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
        return new AftershockTest(testingFigure, attribute, sourceFigure, testCount);
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