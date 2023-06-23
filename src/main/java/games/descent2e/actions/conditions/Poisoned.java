package games.descent2e.actions.conditions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.DescentAction;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;

import static games.descent2e.actions.Triggers.*;

public class Poisoned extends DescentAction {

    public Poisoned() {
        super(FORCED);
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
    public boolean execute(DescentGameState dgs) {

        Figure f = dgs.getActingFigure();

        // Poisoned tests against Might
        Figure.Attribute attribute = Figure.Attribute.Might;
        //System.out.println((attribute));

        AttributeTest attributeTest = new AttributeTest(f.getComponentID());
        attributeTest.setAttribute(attribute);
        attributeTest.execute(dgs);
        boolean result = attributeTest.getResult();

        if (result) {
            f.removeCondition(DescentTypes.DescentCondition.Poison);
            System.out.println("Passed Poisoned Test!");
        }
        else {
            if (!f.getAttribute(Figure.Attribute.Health).isMinimum()) {
                f.getAttribute(Figure.Attribute.Health).decrement();
                System.out.println("Failed Poisoned Test!");
            }
        }

        f.addAttributeTest(DescentTypes.DescentCondition.Poison);

        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        // We can only make one Poisoned attribute test per turn - if we have already taken it, we can't make another attempt
        return f.hasCondition(DescentTypes.DescentCondition.Poison) && !f.hasAttributeTest(DescentTypes.DescentCondition.Poison) && f.getNActionsExecuted().isMinimum();
    }
}
