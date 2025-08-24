package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import java.util.Objects;

public class PromotionTest extends AttributeTest {

    int targetFigure;
    public PromotionTest(int eliza, Figure.Attribute attribute, int targetFigure) {
        // As this test targets the user, we count the source figure as itself
        super(eliza, attribute, eliza);
        this.targetFigure = targetFigure;
        attributeTestName = "Promotion (Willpower) Test: " + eliza + "; Target: " + targetFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(this.getTestingFigure());
        Figure target = (Figure) gameState.getComponentById(targetFigure);

        String testingName = f.getName().replace("Hero: ", "");
        String targetName = target.getName().replace("Hero: ", "");

        return "Promotion (Willpower) Test by " + testingName + " targeting " + targetName;
    }

    @Override
    public String toString() {
        return "Promotion (Willpower) Test by " + super.getSourceFigure() + " targeting " + targetFigure;
    }

    @Override
    public String toStringWithResult() {
        return this + " - " + getTestingName() + " (" + (result ? "Passed; Promote " + targetFigure + " to Master)" : "Failed)");
    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            //System.out.println("Passed Promotion (Willpower) Test!");
        }
        else
        {
            //System.out.println("Failed Promotion (Willpower) Test!");
        }
    }

    @Override
    public PromotionTest _copy() {
        return new PromotionTest(sourceFigure, attribute, targetFigure);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof PromotionTest test) {
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