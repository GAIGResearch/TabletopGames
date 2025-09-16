package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.Move;
import games.descent2e.components.Figure;

import java.util.Objects;

public class CrippleTest extends AttributeTest {

    int targetFigure;
    public CrippleTest(int hero, Figure.Attribute attribute, int targetFigure) {
        // As this test targets the user, we count the source figure as itself
        super(hero, attribute, hero);
        this.targetFigure = targetFigure;
        attributeTestName = "Cripple (Might) Test: " + hero + "; Target: " + targetFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(this.getTestingFigure());
        Figure target = (Figure) gameState.getComponentById(targetFigure);

        String testingName = f.getName().replace("Hero: ", "");
        String targetName = target.getName().replace("Hero: ", "");

        return "Cripple (Might) Test by " + testingName + " targeting " + targetName;
    }

    @Override
    public String toString() {
        return "Cripple (Might) Test by " + super.getSourceFigure() + " targeting " + targetFigure;
    }

    @Override
    public String toStringWithResult() {
        return this + " - " + getTestingName() + " (" + (result ? "Passed; Immobolize " + targetFigure + ")" : "Failed)");
    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            //System.out.println("Passed Cripple (Might) Test!");
            Figure target = (Figure) dgs.getComponentById(targetFigure);
            target.addCondition(DescentTypes.DescentCondition.Immobilize);
            target.setAttributeToMin(Figure.Attribute.MovePoints);
        }
        else
        {
            //System.out.println("Failed Cripple (Might) Test!");
        }
    }

    @Override
    public CrippleTest _copy() {
        return new CrippleTest(sourceFigure, attribute, targetFigure);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof CrippleTest test) {
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