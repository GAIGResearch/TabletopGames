package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;

// TODO REMOVE WHEN NOT DEBUGGING
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;

import java.util.Random;

public class EndTurn extends DescentAction{
    public EndTurn() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "End turn";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Figure f = gs.getActingFigure();
        f.getNActionsExecuted().setToMax();

        // Removes all attribute tests taken this turn from the list, so we can check them again next turn
        f.clearAttributeTest();

        // If we are Immobilized, remove that condition now
        if(f.hasCondition(DescentTypes.DescentCondition.Immobilize)) { f.removeCondition(DescentTypes.DescentCondition.Immobilize); }

        // TODO REMOVE WHEN NOT DEBUGGING
        /*if (f instanceof Hero)
        {
            debug_RandomStatus(f);
        }*/

        System.out.println("End turn for " + f.getName() + " (" + f.getComponentID() + ")");#
        gs.getTurnOrder().endPlayerTurn(gs);
        return true;
    }

    // TODO Delete after debugging, don't include in the final version
    public void debug_RandomStatus(Figure f)
    {
        Random random = new Random();
        int randInt = random.nextInt(2);

        switch(randInt)
        {
            case 0:
                if(!f.hasCondition(DescentTypes.DescentCondition.Disease))
                {
                    f.addCondition(DescentTypes.DescentCondition.Disease);
                    System.out.println("Added Diseased");
                    break;
                }
            case 1:
                if(!f.hasCondition(DescentTypes.DescentCondition.Poison))
                {
                    f.addCondition(DescentTypes.DescentCondition.Poison);
                    System.out.println("Added Poisoned");
                    break;
                }
            case 2:
                if(!f.hasCondition(DescentTypes.DescentCondition.Stun))
                {
                    f.addCondition(DescentTypes.DescentCondition.Stun);
                    System.out.println("Added Stun");
                    break;
                }
            case 3:
                if(!f.hasCondition(DescentTypes.DescentCondition.Immobilize))
                {
                    f.addCondition(DescentTypes.DescentCondition.Immobilize);
                    System.out.println("Added Immobilized");
                    break;
                }
        }
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return true;
    }
}
