package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.TriggerAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.*;

import static games.descent2e.DescentHelper.checkAdjacent;
import static games.descent2e.DescentHelper.getNeighboursInRange;
import static games.descent2e.actions.attack.TriggerAttributeTest.GetAttributeTests.*;

public class Throw extends TriggerAttributeTest {

    protected final int distance = 3; // How far the Monster can throw the target
    public Throw(int attackingFigure, int target) {
        super(attackingFigure, target);
    }

    @Override
    public String getString(AbstractGameState gameState) {

        String attackerName = ((Figure) gameState.getComponentById(attackingFigure)).getName().replace("Hero: ", "");;
        String defenderName = ((Figure) gameState.getComponentById(defendingFigure)).getName().replace("Hero: ", "");

        return "Throw by "+ attackerName + " on " + defenderName;
    }

    @Override
    public String toString() {
        return String.format("Throw by %d on %d", attackingFigure, defendingFigure);
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

        ThrowTest throwTest = new ThrowTest(defendingFigure, Figure.Attribute.Might, attackingFigure, monster.getNActionsExecuted().getValue());
        if (throwTest.canExecute(dgs)) {
            retVal.add(throwTest);
        }

        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        if (target.isOffMap()) {
            Set<BoardNode> targetSpaces = getNeighboursInRange(dgs, target.getPosition(), distance);

            for (BoardNode space : targetSpaces) {
                if (space == null) continue;
                ;
                Vector2D pos = ((PropertyVector2D) space.getProperty("coordinates")).values;
                ThrowMove throwMove = new ThrowMove(defendingFigure, attackingFigure, pos);
                if (throwMove.canExecute(dgs))
                    retVal.add(throwMove);
            }
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
    public Throw copy() {
        Throw retVal = new Throw(attackingFigure, defendingFigure);
        super.copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (!(f instanceof Monster)) return false;
        if (!((Monster) f).hasAction(MonsterAbilities.MonsterAbility.THROW)) return false;
        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        if (target == null) return false;
        return checkAdjacent(dgs, f, target);
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
