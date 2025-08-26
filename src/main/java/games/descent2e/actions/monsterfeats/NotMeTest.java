package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

import java.util.Objects;

public class NotMeTest extends AttributeTest {

    public NotMeTest(int splig, Figure.Attribute attribute) {
        // As this test targets the user, we count the source figure as itself
        super(splig, attribute, splig);
        attributeTestName = "Not Me! (Awareness) Test: " + splig;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(this.getTestingFigure());
        String testingName = f.getName().replace("Hero: ", "");

        return "Not Me! (Awareness) Test by " + testingName;
    }

    @Override
    public String toString() {
        return "Not Me! (Awareness) Test by " + super.getSourceFigure();
    }

    @Override
    public String toStringWithResult() {
        return this + " - " + getTestingName() + " (" + (result ? "Passed; Choose an adjacent Monster to take the attack instead)" : "Failed)");
    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            //System.out.println("Passed Not Me! (Awareness) Test!");
        }
        else
        {
            //System.out.println("Failed Not Me! (Awareness) Test!");
        }
    }

    @Override
    public NotMeTest _copy() {
        return new NotMeTest(sourceFigure, attribute);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof NotMeTest test) {
            return super.equals(test);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}