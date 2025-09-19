package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.getFigureIndex;

public class CaltropsTest extends AttributeTest {

    int targetFigure;
    public CaltropsTest(int hero, Figure.Attribute attribute, int targetFigure) {
        // As this test targets the user, we count the source figure as itself
        super(hero, attribute, hero);
        this.targetFigure = targetFigure;
        attributeTestName = "Caltrops (Awareness) Test: " + hero + "; Target: " + targetFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(this.getTestingFigure());
        Figure target = (Figure) gameState.getComponentById(targetFigure);

        String testingName = f.getName().replace("Hero: ", "");
        String targetName = target.getName().replace("Hero: ", "");

        return "Caltrops (Awareness) Test by " + testingName + " targeting " + targetName;
    }

    @Override
    public String toString() {
        return "Caltrops (Awareness) Test by " + super.getSourceFigure() + " targeting " + targetFigure;
    }

    @Override
    public String toStringWithResult() {
        return this + " - " + getTestingName() + " (" + (result ? "Passed; +1 Damage and Immobolize " + targetFigure + ")" : "Failed)");
    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            //System.out.println("Passed Caltrops (Awareness) Test!");
            Figure target = (Figure) dgs.getComponentById(targetFigure);
            target.addCondition(DescentTypes.DescentCondition.Immobilize);
            target.setAttributeToMin(Figure.Attribute.MovePoints);

            target.getAttribute(Figure.Attribute.Health).decrement();

            if (target.getAttribute(Figure.Attribute.Health).isMinimum())
            {
                int index1 = getFigureIndex(dgs, target);
                int index2 = getFigureIndex(dgs, f);

                // Death
                DescentHelper.figureDeath(dgs, target);

                // Add to the list of defeated figures this turn
                dgs.addDefeatedFigure(target, index1, f, index2);
            }

        }
        else
        {
            //System.out.println("Failed Caltrops (Awareness) Test!");
        }
    }

    @Override
    public CaltropsTest _copy() {
        return new CaltropsTest(sourceFigure, attribute, targetFigure);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof CaltropsTest test) {
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