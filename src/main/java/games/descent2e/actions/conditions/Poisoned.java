package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.Move;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;

import java.util.Objects;

public class Poisoned extends AttributeTest {
    String attributeTestName = "Poisoned";
    public Poisoned(int testingFigure, Figure.Attribute attribute) {
        super(testingFigure, attribute);
        super.setSourceFigure(testingFigure);
        super.setTestCount(0);
    }

    public void announceTestDebug (DescentGameState dgs)
    {
        System.out.println(((Figure) dgs.getComponentById(super.getTestingFigure())).getName() + " must make a Poisoned Test!");
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
        if (result)
        {
            f.removeCondition(DescentTypes.DescentCondition.Poison);
            System.out.println("Passed Poisoned Test!");
        }
        else
        {
            if (!f.getAttribute(Figure.Attribute.Health).isMinimum())
            {
                f.getAttribute(Figure.Attribute.Health).decrement();
                System.out.println("Failed Poisoned Test!");

                // If the figure is defeated by being Poisoned, remove it from the board
                if(f.getAttribute(Figure.Attribute.Health).isMinimum())
                {
                    if (f instanceof Hero)
                    {
                        ((Hero) f).setDefeated(dgs,true);
                    }
                    else if (f instanceof Monster)
                    {
                        Move.remove(dgs, f);
                    }
                }
            }
        }

        f.addAttributeTest(this);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        // We can only make one Poisoned attribute test per turn - if we have already taken it, we can't make another attempt
        return f.hasCondition(DescentTypes.DescentCondition.Poison) && !f.hasAttributeTest(this) && f.getNActionsExecuted().isMinimum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Poisoned poisoned = (Poisoned) o;
        return Objects.equals(attributeTestName, poisoned.attributeTestName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attributeTestName);
    }
}