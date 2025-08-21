package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.Move;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;

import java.util.Objects;

public class Poisoned extends AttributeTest {
    public Poisoned(int testingFigure, Figure.Attribute attribute) {
        super(testingFigure, attribute, testingFigure);
        attributeTestName = "Poisoned Test: " + testingFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Poison Attribute Test";
    }

    @Override
    public void resolveTest(DescentGameState dgs, int figureID, boolean result)
    {
        Figure f = (Figure) dgs.getComponentById(figureID);

        f.addAttributeTest(this);

        if (result)
        {
            f.removeCondition(DescentTypes.DescentCondition.Poison);
            f.addActionTaken("Passed Poisoned Test");
            //System.out.println("Passed Poisoned Test!");
        }
        else
        {
            if (!f.getAttribute(Figure.Attribute.Health).isMinimum())
            {
                f.getAttribute(Figure.Attribute.Health).decrement();
                f.addActionTaken("Failed Poisoned Test");
                //System.out.println("Failed Poisoned Test!");

                // If the figure is defeated by being Poisoned, remove it from the board
                if(f.getAttribute(Figure.Attribute.Health).isMinimum())
                {
                    int index = DescentHelper.getFigureIndex(dgs, f);
                    f.addActionTaken("Defeated by Poison");
                    DescentHelper.figureDeath(dgs, f);
                    dgs.addDefeatedFigure(f, index, "Poisoned");
                }
            }
        }
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        // We can only make one Poisoned attribute test per turn - if we have already taken it, we can't make another attempt
        return f.hasCondition(DescentTypes.DescentCondition.Poison) && !f.hasAttributeTest(this) && f.getNActionsExecuted().isMinimum();
    }

    @Override
    public Poisoned _copy() {
        return new Poisoned(testingFigure, attribute);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof Poisoned;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 28092;
    }
}
