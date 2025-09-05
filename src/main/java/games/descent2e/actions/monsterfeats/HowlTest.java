package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

public class HowlTest extends AttributeTest {

    public HowlTest(int testingFigure, Figure.Attribute attribute, int sourceFigure) {
        super(testingFigure, attribute, sourceFigure);
        attributeTestName = "Howl (Willpower) Test: " + sourceFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Howl (Willpower) Test by " + super.getSourceFigure() + " on " + super.getTestingFigure();
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
            //System.out.println("Passed Howl (Willpower) Test!");
        }
        else
        {
            //System.out.println("Failed Howl (Willpower) Test!");
            String test = "Howl of " + ((Figure) dgs.getComponentById(super.getSourceFigure())).getName();
            DescentHelper.forcedFatigue(dgs, f, test);
        }
    }

    @Override
    public HowlTest _copy() {
        return new HowlTest(testingFigure, attribute, sourceFigure);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof HowlTest ht) {
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