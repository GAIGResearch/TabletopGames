package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import java.util.Objects;

public class SeduceTest extends AttributeTest {

    int targetFigure;
    public SeduceTest(int eliza, Figure.Attribute attribute, int targetFigure, int testCount) {
        // As this test targets the user, we count the source figure as itself
        super(eliza, attribute, eliza, testCount);
        this.targetFigure = targetFigure;
        attributeTestName = "Seduce (Willpower) Test: " + eliza + "; Target: " + targetFigure + "-" + testCount;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Seduce (Willpower) Test (" + super.getTestCount() + ") by " + super.getSourceFigure();
    }

    @Override
    public String toStringWithResult() {
        return this + " - " + getTestingName() + " (" + (result ? "Passed; Force Move and Stun " + targetFigure + ")" : "Failed)");
    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            //System.out.println("Passed Seduce (Willpower) Test!");
            Figure target = (Figure) dgs.getComponentById(targetFigure);
            target.setOffMap(true);
            target.getAttribute(Figure.Attribute.MovePoints).setValue(1);
        }
        else
        {
            //System.out.println("Failed Seduce (Willpower) Test!");
        }
    }

    @Override
    public SeduceTest _copy() {
        return new SeduceTest(sourceFigure, attribute, targetFigure, testCount);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof SeduceTest test) {
            return super.equals(test) && this.targetFigure == test.targetFigure;
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

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetFigure);
    }

}