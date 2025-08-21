package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import java.util.Objects;

public class DominionTest extends AttributeTest {

    int targetFigure;
    boolean partTwo;

    public DominionTest (int targetFigure, Figure.Attribute attribute, int zachareth)
    {
        // As this test targets the user, we count the source figure as itself
        super(zachareth, attribute, zachareth);
        this.targetFigure = targetFigure;
        this.partTwo = false;
        attributeTestName = "Dominion (Willpower) Test: " + zachareth;
    }
    public DominionTest(int targetFigure, Figure.Attribute attribute, int zachareth, boolean partTwo) {
        super(targetFigure, attribute, zachareth);
        this.targetFigure = targetFigure;
        this.partTwo = partTwo;
        attributeTestName = "Dominion (Willpower) Test: " + zachareth;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(this.getTestingFigure());
        Figure target = (Figure) gameState.getComponentById(this.getSourceFigure());

        String testingName = f.getName().replace("Hero: ", "");
        String targetName = target.getName().replace("Hero: ", "");

        if (partTwo) return "Dominion (Willpower) Test by " + testingName + " on " + targetName;
        else return "Dominion (Willpower) Test by " + testingName + " targeting " + targetName;
    }

    @Override
    public String toString() {
        if (partTwo) return "Dominion (Willpower) Test by " + super.getSourceFigure() + " on " + targetFigure;
        else return "Dominion (Willpower) Test by " + super.getSourceFigure() + " targeting " + targetFigure;
    }

    @Override
    public String toStringWithResult() {
        if (!partTwo) return this + " - " + getTestingName() + " (" + (result ? "Passed; Force Move and Immobilize (Willpower) Test " + targetFigure + ")" : "Failed)");
        else return this + " - " + getTestingName() + " (" + (result ? "Passed)" : "Failed; Immobilized)");

    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            System.out.println("Passed Dominion (Willpower) Test!");

            // This is only for Baron Zachareth's test on himself
            // Nothing happens if the second test is passed
            if (!partTwo) {
                Figure target = (Figure) dgs.getComponentById(targetFigure);
                target.setOffMap(true);
                target.getAttribute(Figure.Attribute.MovePoints).setValue(2);
            }
        }
        else
        {
            System.out.println("Failed Dominion (Willpower) Test!");

            // This is only for the target's test as the second part
            // Nothing happens if the first test is failed
            if (partTwo)
            {
                f.addCondition(DescentTypes.DescentCondition.Immobilize);
            }
        }
    }

    @Override
    public DominionTest _copy() {
        DominionTest retVal;
        if (partTwo) retVal = new DominionTest(targetFigure, attribute, sourceFigure, partTwo);
        else retVal = new DominionTest(targetFigure, attribute, sourceFigure);
        return retVal;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof DominionTest test) {
            return super.equals(test) && this.targetFigure == test.targetFigure && this.partTwo == test.partTwo;
        } else {
            return false;
        }
    }
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetFigure, partTwo);
    }
}