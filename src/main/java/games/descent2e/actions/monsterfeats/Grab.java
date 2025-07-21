package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.TriggerAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentTypes.DescentCondition.Immobilize;
import static games.descent2e.actions.attack.TriggerAttributeTest.GetAttributeTests.POST_TEST;
import static games.descent2e.actions.attack.TriggerAttributeTest.GetAttributeTests.PRE_TEST;

public class Grab extends TriggerAttributeTest {

    public Grab(int attackingFigure, Integer target) {
        super(attackingFigure, target);
    }

    @Override
    public String getString(AbstractGameState gameState) {

        String attackerName = ((Figure) gameState.getComponentById(attackingFigure)).getName().replace("Hero: ", "");;
        String defenderName = ((Figure) gameState.getComponentById(defendingFigure)).getName().replace("Hero: ", "");

        return "Grab by "+ attackerName + " on " + defenderName;
    }

    @Override
    public String toString() {
        return String.format("Grab by %d on %d", attackingFigure, defendingFigure);
    }

    @Override
    public boolean execute(DescentGameState state) {
        super.execute(state);
        Figure monster = (Figure) state.getComponentById(attackingFigure);
        monster.getNActionsExecuted().increment();
        monster.setHasAttacked(true);
        monster.addActionTaken(toString());

        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        if (phase.interrupt == null)
            throw new AssertionError("Should not be reachable");
        DescentGameState dgs = (DescentGameState) state;
        Monster monster = (Monster) dgs.getComponentById(attackingFigure);
        List<AbstractAction> retVal = new ArrayList<>();

        // System.out.println(((Figure) dgs.getComponentById(defendingFigure)).getName());

        GrabTest grabTest = new GrabTest(defendingFigure, Figure.Attribute.Might, attackingFigure, monster.getNActionsExecuted().getValue());

        if (grabTest.canExecute(dgs)) {
            retVal.add(grabTest);
        }

        if (retVal.isEmpty())
        {
            List<AbstractAction> superRetVal = super._computeAvailableActions(state);
            if (superRetVal != null && !superRetVal.isEmpty())
            {
                retVal.addAll(superRetVal);
            }
        }

        return retVal;
    }

    @Override
    public Grab copy() {
        Grab retVal = new Grab(attackingFigure, defendingFigure);
        super.copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        Figure target = (Figure) dgs.getComponentById(defendingFigure);

        // As identical conditions do not stack, there is no point attempting to grab a figure that is already immobilized
        if (target == null || target.hasCondition(Immobilize)) {
            return false;
        }
        return f instanceof Monster && (((Monster) f).hasAction(MonsterAbilities.MonsterAbility.GRAB));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}
