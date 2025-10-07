package games.descent2e.actions.items;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import java.util.Objects;

import static games.descent2e.DescentHelper.getFigureIndex;

public class ScorpionsKissTest extends AttributeTest {

    DescentTypes.DescentCondition condition;
    public ScorpionsKissTest(int testingFigure, Figure.Attribute attribute, int sourceFigure, DescentTypes.DescentCondition condition) {
        super(testingFigure, attribute, sourceFigure);
        this.condition = condition;
        attributeTestName = "Scorpion's Kiss (Awareness) Test: " + sourceFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String retVal = super.getString(gameState);
        return condition.toString() + ": Scorpion's Kiss (Awareness) Test by" + retVal.split("Test by")[1];
    }

    @Override
    public String toString() {
        String retVal = super.toString();
        return condition.toString() + ": Scorpion's Kiss (Awareness) Test by" + retVal.split("Test by")[1];
    }

    @Override
    public String toStringWithResult() {
        return this + " - " + getTestingName() + " (" + (result ? "Passed; " + condition.toString() : "Failed; Immobilize and Poison)");
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        // Only allow this test if the target does not have this condition at the moment
        // If the target has both (i.e. Immobilize and Poison), then they should not be a legal target anyway
        return (!((Figure) dgs.getComponentById(testingFigure)).hasCondition(condition));
    }


    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            //System.out.println("Passed Scorpion's Kiss (Awareness) Test!");
            f.addCondition(condition);

        }
        else
        {
            //System.out.println("Failed Scorpion's Kiss (Awareness) Test!");
            f.addCondition(DescentTypes.DescentCondition.Immobilize);
            f.addCondition(DescentTypes.DescentCondition.Poison);
        }
    }

    @Override
    public ScorpionsKissTest _copy() {
        return new ScorpionsKissTest(testingFigure, attribute, sourceFigure, condition);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ScorpionsKissTest test) {
            return super.equals(test) && condition == test.condition;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), condition);
    }
}