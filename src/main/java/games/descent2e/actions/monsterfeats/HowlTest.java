package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.components.Figure;

public class HowlTest extends AttributeTest {

    String attributeTestName = "Howl";
    public HowlTest(int testingFigure, Figure.Attribute attribute) {
        super(testingFigure, attribute);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Howl (Willpower) Test";
    }

    @Override
    public void resolveTest(Figure f, boolean result)
    {
        if (result)
        {
            System.out.println("Passed Howl (Willpower) Test!");
        }
        else
        {
            if (!f.getAttribute(Figure.Attribute.Fatigue).isMaximum())
            {
                f.getAttribute(Figure.Attribute.Fatigue).increment();
                System.out.println("Failed Howl (Willpower) Test!");
            }
        }

        String source = String.valueOf((this.getSourceFigure().getComponentID()));
        String count = String.valueOf((this.getTestCount()));
        setAttributeTestName(source + "-" + count);

        f.addAttributeTest(attributeTestName);
    }

    @Override
    public void setAttributeTestName(String name)
    {
        attributeTestName = "Howl (Willpower) Test: " + name;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.getTestingFigure());
        // We can only make each attribute test once per turn - if we have already taken it, we can't make another attempt
        return !f.hasAttributeTest(attributeTestName) && f.getNActionsExecuted().isMinimum();
    }
}