package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import java.util.Objects;

public class OverpowerTest extends AttributeTest {

    int targetFigure;
    public OverpowerTest(int alric, Figure.Attribute attribute, int targetFigure) {
        // As this test targets the user, we count the source figure as itself
        super(alric, attribute, alric);
        this.targetFigure = targetFigure;
        attributeTestName = "Overpower (Might) Test: " + alric + "; Target: " + targetFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(this.getTestingFigure());
        Figure target = (Figure) gameState.getComponentById(targetFigure);

        String testingName = f.getName().replace("Hero: ", "");
        String targetName = target.getName().replace("Hero: ", "");

        return "Overpower (Might) Test by " + testingName + " targeting " + targetName;
    }

    @Override
    public String toString() {
        return "Overpower (Might) Test by " + super.getSourceFigure() + " targeting " + targetFigure;
    }

    @Override
    public String toStringWithResult() {
        return this + " - " + getTestingName() + " (" + (result ? "Passed; Force Swap and Inflict 1 Fatigue " + targetFigure + ")" : "Failed)");
    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            // System.out.println("Passed Overpower (Might) Test!");
            Figure target = (Figure) dgs.getComponentById(targetFigure);
            f.setOffMap(true);
            target.setOffMap(true);
        }
        else
        {
            // System.out.println("Failed Overpower (Might) Test!");
        }
    }

    @Override
    public OverpowerTest _copy() {
        return new OverpowerTest(sourceFigure, attribute, targetFigure);
    }

    public int getTargetFigure() {
        return targetFigure;
    }
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof OverpowerTest test) {
            return super.equals(test) && this.targetFigure == test.targetFigure;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetFigure);
    }
}